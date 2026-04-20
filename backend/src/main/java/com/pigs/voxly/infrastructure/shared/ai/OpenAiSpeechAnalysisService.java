package com.pigs.voxly.infrastructure.shared.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pigs.voxly.application.shared.ports.SpeechAnalysisService;
import com.pigs.voxly.application.shared.ports.TranscriptionService;
import com.pigs.voxly.sharedKernel.domain.results.Error;
import com.pigs.voxly.sharedKernel.domain.results.ResultT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAiSpeechAnalysisService implements SpeechAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiSpeechAnalysisService.class);
    private static final Pattern FILLER_PATTERN = Pattern.compile(
            "\\b(um|uh|like|basically|so|actually|you know|I mean|kind of|sort of)\\b",
            Pattern.CASE_INSENSITIVE
    );

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
            String sessionType
    ) {
        log.info("Starting OpenAI analysis for {} session, duration: {}s", sessionType, transcription.durationSeconds());

        String text = transcription.fullText();
        List<TranscriptionService.Segment> segments = transcription.segments();

        // Calculate metrics locally (deterministic, no need for AI)
        int totalWords = countWords(text);
        double durationMinutes = transcription.durationSeconds() / 60.0;
        int wordsPerMinute = durationMinutes > 0 ? (int) Math.round(totalWords / durationMinutes) : 0;

        List<FillerWordOccurrence> fillerWords = findFillerWords(segments);
        int fillerWordCount = fillerWords.stream().mapToInt(FillerWordOccurrence::count).sum();

        List<PauseOccurrence> pauses = findPauses(segments);
        double avgSentenceLength = calculateAverageSentenceLength(text);
        double clarityScore = calculateClarityScore(wordsPerMinute, fillerWordCount, totalWords);

        Metrics metrics = new Metrics(
                wordsPerMinute, totalWords, durationMinutes,
                fillerWordCount, fillerWords,
                pauses.size(), pauses,
                avgSentenceLength, clarityScore
        );

        if (aiProperties.apiKey() == null || aiProperties.apiKey().isBlank()) {
            return ResultT.failure(Error.failure(
                    "Analysis.OpenAiNotConfigured",
                    "OPENAI_API_KEY is not configured"
            ));
        }

        // Use GPT for qualitative feedback
        try {
            var gptFeedback = getGptFeedback(transcription, sessionType, metrics);

            var result = new AnalysisResult(
                    metrics,
                    combineFeedbackNotes(fillerWords, pauses, gptFeedback.feedbackNotes),
                    gptFeedback.overallSummary,
                    gptFeedback.strengths,
                    gptFeedback.areasForImprovement
            );

            log.info("OpenAI analysis completed: {} WPM, {} filler words, {} clarity",
                    wordsPerMinute, fillerWordCount, clarityScore);

            return ResultT.success(result);

        } catch (Exception e) {
            log.error("GPT analysis failed", e);
            return ResultT.failure(Error.failure(
                    "Analysis.Failed",
                    "OpenAI analysis failed: " + summarizeException(e)
            ));
        }
    }

    private GptFeedback getGptFeedback(
            TranscriptionService.TranscriptionResult transcription,
            String sessionType,
            Metrics metrics
    ) throws JsonProcessingException {
        String prompt = buildPrompt(transcription, sessionType, metrics);

        var requestBody = Map.of(
                "model", aiProperties.gpt().model(),
                "temperature", 0.7,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", prompt)
                )
        );

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
            Metrics metrics
    ) {
        return String.format("""
                Analyze this %s transcription and provide feedback.

                **Metrics:**
                - Words per minute: %d
                - Total words: %d
                - Filler words: %d
                - Pauses: %d
                - Duration: %.1f minutes

                **Transcription:**
                %s

                Respond in this exact JSON format:
                {
                  "overallSummary": "2-3 sentence summary of the performance",
                  "strengths": ["strength 1", "strength 2", "strength 3"],
                  "areasForImprovement": ["improvement 1", "improvement 2"],
                  "feedbackNotes": [
                    {
                      "category": "pacing|clarity|engagement|structure|content",
                      "severity": "info|warning|suggestion",
                      "message": "specific actionable feedback"
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
                transcription.fullText()
        );
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
            List<FillerWordOccurrence> fillerWords,
            List<PauseOccurrence> pauses,
            List<GptFeedbackNote> gptNotes
    ) {
        List<FeedbackNote> notes = new ArrayList<>();

        // Timestamped filler word notes
        for (var filler : fillerWords) {
            notes.add(new FeedbackNote(
                    "filler", "warning",
                    "Filler word detected: \"" + filler.word() + "\"",
                    filler.timestampSeconds(), null
            ));
        }

        // Timestamped pause notes
        for (var pause : pauses) {
            if ("awkward".equals(pause.type())) {
                notes.add(new FeedbackNote(
                        "pacing", "warning",
                        String.format("Long pause (%.1fs). Consider smoother transitions.", pause.durationSeconds()),
                        pause.startSeconds(), pause.endSeconds()
                ));
            }
        }

        // GPT qualitative notes
        if (gptNotes != null) {
            for (var note : gptNotes) {
                notes.add(new FeedbackNote(
                        note.category != null ? note.category : "suggestion",
                        note.severity != null ? note.severity : "info",
                        note.message,
                        null, null
                ));
            }
        }

        return notes;
    }

    // --- Local metric calculations (same as MockSpeechAnalysisService) ---

    private int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.split("\\s+").length;
    }

    private List<FillerWordOccurrence> findFillerWords(List<TranscriptionService.Segment> segments) {
        List<FillerWordOccurrence> occurrences = new ArrayList<>();
        for (var segment : segments) {
            Matcher matcher = FILLER_PATTERN.matcher(segment.text());
            while (matcher.find()) {
                occurrences.add(new FillerWordOccurrence(matcher.group().toLowerCase(), segment.startSeconds(), 1));
            }
        }
        return occurrences;
    }

    private List<PauseOccurrence> findPauses(List<TranscriptionService.Segment> segments) {
        List<PauseOccurrence> pauses = new ArrayList<>();
        for (int i = 1; i < segments.size(); i++) {
            double gap = segments.get(i).startSeconds() - segments.get(i - 1).endSeconds();
            if (gap >= 1.5) {
                String type = gap > 3.0 ? "awkward" : (gap > 2.0 ? "natural" : "dramatic");
                pauses.add(new PauseOccurrence(segments.get(i - 1).endSeconds(), segments.get(i).startSeconds(), gap, type));
            }
        }
        return pauses;
    }

    private double calculateAverageSentenceLength(String text) {
        if (text == null || text.isBlank()) return 0;
        String[] sentences = text.split("[.!?]+");
        int total = 0;
        for (String s : sentences) total += countWords(s.trim());
        return sentences.length > 0 ? (double) total / sentences.length : 0;
    }

    private double calculateClarityScore(int wpm, int fillerCount, int totalWords) {
        double wpmScore = (wpm >= 120 && wpm <= 150) ? 1.0 :
                (wpm < 100 || wpm > 180) ? 0.7 : 0.85;
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
        public String message;
    }
}
