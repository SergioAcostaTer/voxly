package com.pigs.voxly.infrastructure.shared.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pigs.voxly.application.shared.ports.SpeechAnalysisService;
import com.pigs.voxly.application.shared.ports.TranscriptionService;
import com.pigs.voxly.sharedKernel.domain.results.Error;
import com.pigs.voxly.sharedKernel.domain.results.ResultT;

@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAiSpeechAnalysisService implements SpeechAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiSpeechAnalysisService.class);
    private static final Pattern FILLER_PATTERN = Pattern.compile(
            "\\b(um|uh|like|basically|so|actually|you know|I mean|kind of|sort of)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final int MIN_FEEDBACK_NOTES = 6;

    private final RestClient restClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public OpenAiSpeechAnalysisService(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .defaultHeader("Authorization", "Bearer " + aiProperties.apiKey())
                .build();
    }

    @Override
    public ResultT<AnalysisResult> analyze(
            TranscriptionService.TranscriptionResult transcription,
            String sessionType) {
        log.info("Starting OpenAI analysis for {} session, duration: {}s", sessionType,
                transcription.durationSeconds());

        String text = transcription.fullText();
        List<TranscriptionService.Segment> segments = transcription.segments();

        // Calculate metrics locally (deterministic, no need for AI)
        int totalWords = countWords(text);
        double durationMinutes = transcription.durationSeconds() / 60.0;
        int wordsPerMinute = durationMinutes > 0 ? (int) Math.round(totalWords / durationMinutes) : 0;

        List<FillerWordOccurrence> fillerWords = findFillerWords(transcription);
        int fillerWordCount = fillerWords.stream().mapToInt(FillerWordOccurrence::count).sum();

        List<PauseOccurrence> pauses = findPauses(segments);
        double avgSentenceLength = calculateAverageSentenceLength(text);
        double clarityScore = calculateClarityScore(wordsPerMinute, fillerWordCount, totalWords);

        Metrics metrics = new Metrics(
                wordsPerMinute, totalWords, durationMinutes,
                fillerWordCount, fillerWords,
                pauses.size(), pauses,
                avgSentenceLength, clarityScore);

        if (aiProperties.apiKey() == null || aiProperties.apiKey().isBlank()) {
            return ResultT.failure(Error.failure(
                    "Analysis.OpenAiNotConfigured",
                    "OPENAI_API_KEY is not configured"));
        }

        // Use GPT for qualitative feedback
        try {
            var gptFeedback = getGptFeedback(transcription, sessionType, metrics);

            var result = new AnalysisResult(
                    metrics,
                    combineFeedbackNotes(transcription, metrics, fillerWords, pauses, gptFeedback.feedbackNotes),
                    gptFeedback.overallSummary,
                    gptFeedback.strengths,
                    gptFeedback.areasForImprovement);

            log.info("OpenAI analysis completed: {} WPM, {} filler words, {} clarity",
                    wordsPerMinute, fillerWordCount, clarityScore);

            return ResultT.success(result);

        } catch (Exception e) {
            log.error("GPT analysis failed", e);
            return ResultT.failure(Error.failure(
                    "Analysis.Failed",
                    "OpenAI analysis failed: " + summarizeException(e)));
        }
    }

    private GptFeedback getGptFeedback(
            TranscriptionService.TranscriptionResult transcription,
            String sessionType,
            Metrics metrics) throws JsonProcessingException {
        String prompt = buildPrompt(transcription, sessionType, metrics);

        var requestBody = Map.of(
                "model", aiProperties.gpt().model(),
                "temperature", 0.4,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", prompt)));

        String chatUrl = aiProperties.baseUrl() + "/chat/completions";

        var response = restClient.post()
                .uri(chatUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(ChatCompletionResponse.class);

        if (response == null || response.choices == null || response.choices.isEmpty()) {
            throw new RuntimeException("Empty GPT response");
        }

        String content = response.choices.get(0).message.content;
        return parseGptResponse(content);
    }

    private String buildPrompt(
            TranscriptionService.TranscriptionResult transcription,
            String sessionType,
            Metrics metrics) {
        String transcriptPayload = buildTranscriptPayload(transcription);
        return String.format(
                """
                        Analyze this %s transcription and provide feedback.

                        **Metrics:**
                        - Words per minute: %d
                        - Total words: %d
                        - Filler words: %d
                        - Pauses: %d
                        - Duration: %.1f minutes

                        **Transcription:**
                        %s

                        **Output requirements:**
                        - You are writing for Guided Coach Mode. Each note should read like a conversational coaching intervention.
                        - Keep tone friendly, first-person, and professional.
                        - Provide 6 to 8 total notes when the speech has enough material.
                        - Include at least 4 timestamped notes whenever the transcript has clear moments to point to.
                        - Cluster nearby issues into a single feedback note when they happen within 8 seconds.
                        - Prefer precise timestamps with 0.1-second resolution when possible.
                        - Make each note specific and actionable. Avoid vague advice.
                        - Use endTimestampSeconds only when a note spans a longer section.
                        - Keep strengths and improvement bullets concrete and non-overlapping.
                        - Use the timing data below to anchor feedback to the exact word or segment where the issue happens.

                        **Transcript timing data:**
                        %s

                        Respond in this exact JSON format:
                        {
                          "overallSummary": "2-3 sentence summary of the performance",
                          "strengths": ["strength 1", "strength 2", "strength 3"],
                          "areasForImprovement": ["improvement 1", "improvement 2", "improvement 3"],
                          "feedbackNotes": [
                            {
                                                    "category": "pacing|filler|clarity|structure",
                                                    "severity": "info|warning|critical",
                                                    "timestampSeconds": 14.5,
                                                    "endTimestampSeconds": 18.2,
                                                    "title": "Short coaching title",
                                                    "message": "specific actionable feedback tied to this exact moment",
                                                    "coachScript": "First-person conversational coach explanation."
                            }
                          ]
                        }
                        """,
                sessionType,
                metrics.wordsPerMinute(),
                metrics.totalWords(),
                metrics.fillerWordCount(),
                metrics.pauseCount(),
                metrics.durationMinutes(),
                transcriptPayload);
    }

    private GptFeedback parseGptResponse(String content) throws JsonProcessingException {
        // Strip markdown code fences if present
        String json = content.strip();
        if (json.startsWith("```")) {
            json = json.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
        }
        return objectMapper.readValue(json, GptFeedback.class);
    }

    private String summarizeException(Exception exception) {
        if (exception instanceof RestClientResponseException restException) {
            String body = restException.getResponseBodyAsString();
            if (body != null && !body.isBlank()) {
                return truncate(body.replaceAll("\\s+", " ").trim(), 220);
            }
        }
        return truncate(exception.getMessage(), 220);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private List<FeedbackNote> combineFeedbackNotes(
            TranscriptionService.TranscriptionResult transcription,
            Metrics metrics,
            List<FillerWordOccurrence> fillerWords,
            List<PauseOccurrence> pauses,
            List<GptFeedbackNote> gptNotes) {
        List<FeedbackNote> notes = new ArrayList<>();

        // Timestamped filler word notes
        for (var filler : fillerWords) {
            notes.add(new FeedbackNote(
                    "filler", "warning",
                    "Filler word detected: \"" + filler.word() + "\"",
                    filler.timestampSeconds(), null,
                    "Filler word cluster",
                    "I noticed a filler word here. A brief silent pause can sound more confident than filling space with extra words."));
        }

        // Timestamped pause notes
        for (var pause : pauses) {
            if ("awkward".equals(pause.type())) {
                double timestamp = pause.startSeconds() + (pause.durationSeconds() / 2.0);
                notes.add(new FeedbackNote(
                        "pacing", "warning",
                        String.format("Long pause (%.1fs). Consider smoother transitions.", pause.durationSeconds()),
                        timestamp, pause.endSeconds(),
                        "Long pause",
                        "I noticed a longer pause here. A quick transition phrase can keep momentum while you gather your next thought."));
            }
        }

        // GPT qualitative notes
        if (gptNotes != null) {
            for (var note : gptNotes) {
                notes.add(new FeedbackNote(
                        normalizeCategory(note.category),
                        normalizeSeverity(note.severity),
                        note.message,
                        note.timestampSeconds,
                    note.endTimestampSeconds,
                    note.title,
                    note.coachScript));
            }
        }

        if (notes.size() < MIN_FEEDBACK_NOTES) {
            notes.addAll(buildHeuristicNotes(transcription, metrics, notes));
        }

        return notes;
    }

    private List<FeedbackNote> buildHeuristicNotes(
            TranscriptionService.TranscriptionResult transcription,
            Metrics metrics,
            List<FeedbackNote> existingNotes) {
        List<FeedbackNote> notes = new ArrayList<>();
        double midpoint = transcription.durationSeconds() > 0 ? transcription.durationSeconds() / 2.0 : 0.0;
        double firstThird = transcription.durationSeconds() > 0 ? transcription.durationSeconds() / 3.0 : 0.0;
        double secondThird = transcription.durationSeconds() > 0 ? transcription.durationSeconds() * 0.66 : 0.0;

        if (!hasCategory(existingNotes, "pacing")
                && (metrics.wordsPerMinute() < 110 || metrics.wordsPerMinute() > 170)) {
            notes.add(new FeedbackNote(
                    "pacing",
                    "warning",
                    metrics.wordsPerMinute() < 110
                            ? "Your delivery is a little slow in this section."
                            : "Your delivery is moving fast enough that clarity may suffer.",
                    midpoint,
                    null,
                    "Pacing check",
                    metrics.wordsPerMinute() < 110
                            ? "I would tighten the pace here a little. A slightly firmer rhythm can help the audience stay engaged."
                            : "I would slow down just a bit here. That extra breathing room usually makes the point land more cleanly."));
        }

        if (!hasCategory(existingNotes, "structure") && metrics.averageSentenceLength() > 18) {
            notes.add(new FeedbackNote(
                    "structure",
                    "info",
                    "Several ideas are packed into long sentences here.",
                    secondThird,
                    null,
                    "Break the idea up",
                    "I would split this thought into smaller pieces. Clear sections make it easier for the audience to follow the argument."));
        }

        if (!hasCategory(existingNotes, "clarity") && metrics.clarityScore() < 0.85) {
            notes.add(new FeedbackNote(
                    "clarity",
                    "warning",
                    "This section could land with a cleaner, more deliberate explanation.",
                    firstThird,
                    null,
                    "Clarify the point",
                    "I would slow this part down and make the takeaway more explicit so the audience does not have to infer the conclusion."));
        }

        return notes;
    }

    private boolean hasCategory(List<FeedbackNote> notes, String category) {
        return notes.stream().anyMatch(note -> normalizeCategory(note.category()).equals(category));
    }

    private String buildTranscriptPayload(TranscriptionService.TranscriptionResult transcription) {
        try {
            var payload = new TranscriptPayload(
                    transcription.fullText(),
                    transcription.durationSeconds(),
                    transcription.detectedLanguage(),
                    transcription.segments(),
                    transcription.words());
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return transcription.fullText();
        }
    }

    private double estimateTimestampSeconds(TranscriptionService.Segment segment, int startIndex, int endIndex) {
        double start = segment.startSeconds();
        double end = Math.max(segment.endSeconds(), start + 0.1);
        double duration = Math.max(0.1, end - start);
        String text = segment.text() == null ? "" : segment.text();
        if (text.isBlank()) {
            return start;
        }

        double center = (startIndex + Math.max(1, endIndex - startIndex) / 2.0) / Math.max(1, text.length());
        double timestamp = start + (duration * Math.max(0.0, Math.min(1.0, center)));
        return Math.max(start, Math.min(end, timestamp));
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "clarity";
        }

        String normalized = category.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "pacing", "filler", "clarity", "structure" -> normalized;
            default -> "clarity";
        };
    }

    private String normalizeSeverity(String severity) {
        if (severity == null || severity.isBlank()) {
            return "info";
        }

        String normalized = severity.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            // Keep backward compatibility with previously stored notes.
            case "info", "warning", "critical", "suggestion" -> normalized;
            default -> "info";
        };
    }

    // --- Local metric calculations (same as MockSpeechAnalysisService) ---

    private int countWords(String text) {
        if (text == null || text.isBlank())
            return 0;
        return text.split("\\s+").length;
    }

    private List<FillerWordOccurrence> findFillerWords(TranscriptionService.TranscriptionResult transcription) {
        List<FillerWordOccurrence> occurrences = new ArrayList<>();

        if (transcription.words() != null && !transcription.words().isEmpty()) {
            occurrences.addAll(findFillerWordsFromWords(transcription.words()));
            if (!occurrences.isEmpty()) {
                return occurrences;
            }
        }

        for (var segment : transcription.segments()) {
            String text = segment.text() == null ? "" : segment.text();
            Matcher matcher = FILLER_PATTERN.matcher(text);
            while (matcher.find()) {
                occurrences.add(new FillerWordOccurrence(
                        matcher.group().toLowerCase(Locale.ROOT),
                        estimateTimestampSeconds(segment, matcher.start(), matcher.end()),
                        1));
            }
        }
        return occurrences;
    }

    private List<FillerWordOccurrence> findFillerWordsFromWords(List<TranscriptionService.Word> words) {
        List<FillerWordOccurrence> occurrences = new ArrayList<>();
        List<String> normalizedWords = words.stream()
                .map(word -> normalizeToken(word.word()))
                .toList();

        for (int i = 0; i < words.size(); i++) {
            String current = normalizedWords.get(i);
            TranscriptionService.Word word = words.get(i);
            if (current.isBlank()) {
                continue;
            }

            if (isSingleWordFiller(current)) {
                occurrences.add(new FillerWordOccurrence(current, word.startSeconds(), 1));
                continue;
            }

            if (i + 1 < words.size()) {
                String pair = current + " " + normalizedWords.get(i + 1);
                if (isPhraseFiller(pair)) {
                    occurrences.add(new FillerWordOccurrence(pair, word.startSeconds(), 1));
                }
            }
        }

        return occurrences;
    }

    private String normalizeToken(String token) {
        if (token == null) {
            return "";
        }
        return token.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z\\s']", "")
                .trim();
    }

    private boolean isSingleWordFiller(String token) {
        return switch (token) {
            case "um", "uh", "like", "basically", "so", "actually" -> true;
            default -> false;
        };
    }

    private boolean isPhraseFiller(String phrase) {
        return switch (phrase) {
            case "you know", "i mean", "kind of", "sort of" -> true;
            default -> false;
        };
    }

    private List<PauseOccurrence> findPauses(List<TranscriptionService.Segment> segments) {
        List<PauseOccurrence> pauses = new ArrayList<>();
        for (int i = 1; i < segments.size(); i++) {
            double gap = segments.get(i).startSeconds() - segments.get(i - 1).endSeconds();
            if (gap >= 1.5) {
                String type = gap > 3.0 ? "awkward" : (gap > 2.0 ? "natural" : "dramatic");
                pauses.add(new PauseOccurrence(segments.get(i - 1).endSeconds(), segments.get(i).startSeconds(), gap,
                        type));
            }
        }
        return pauses;
    }

    private double calculateAverageSentenceLength(String text) {
        if (text == null || text.isBlank())
            return 0;
        String[] sentences = text.split("[.!?]+");
        int total = 0;
        for (String s : sentences)
            total += countWords(s.trim());
        return sentences.length > 0 ? (double) total / sentences.length : 0;
    }

    private double calculateClarityScore(int wpm, int fillerCount, int totalWords) {
        double wpmScore = (wpm >= 120 && wpm <= 150) ? 1.0 : (wpm < 100 || wpm > 180) ? 0.7 : 0.85;
        double fillerRatio = totalWords > 0 ? (double) fillerCount / totalWords : 0;
        double fillerScore = Math.max(0, 1.0 - (fillerRatio * 10));
        return Math.round((wpmScore * 0.5 + fillerScore * 0.5) * 100) / 100.0;
    }

    // --- Response DTOs ---

    private static final String SYSTEM_PROMPT = """
            You are a professional speech coach. Analyze presentation transcriptions and provide constructive,
            specific, and actionable feedback. Be encouraging but honest. Focus on what matters most for the
            given session type. Always respond with valid JSON only, no extra text.
            """;

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ChatCompletionResponse {
        public List<Choice> choices;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Choice {
        public Message message;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Message {
        public String content;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GptFeedback {
        public String overallSummary;
        public List<String> strengths;
        public List<String> areasForImprovement;
        public List<GptFeedbackNote> feedbackNotes;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GptFeedbackNote {
        public String category;
        public String severity;
        public Double timestampSeconds;
        public Double endTimestampSeconds;
        public String title;
        public String message;
        public String coachScript;
    }

    private record TranscriptPayload(
            String fullText,
            double durationSeconds,
            String detectedLanguage,
            List<TranscriptionService.Segment> segments,
            List<TranscriptionService.Word> words) {
    }
}
