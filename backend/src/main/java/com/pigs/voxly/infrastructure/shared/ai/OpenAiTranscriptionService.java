package com.pigs.voxly.infrastructure.shared.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pigs.voxly.application.shared.ports.TranscriptionService;
import com.pigs.voxly.sharedKernel.domain.results.Error;
import com.pigs.voxly.sharedKernel.domain.results.ResultT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAiTranscriptionService implements TranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiTranscriptionService.class);
    private final RestClient restClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public OpenAiTranscriptionService(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .defaultHeader("Authorization", "Bearer " + aiProperties.apiKey())
                .build();
    }

    @Override
    public ResultT<TranscriptionResult> transcribe(Path filePath, String language) {
        log.info("Starting Whisper transcription for: {}", filePath.getFileName());

        try {
            if (aiProperties.apiKey() == null || aiProperties.apiKey().isBlank()) {
                return ResultT.failure(Error.failure(
                        "Transcription.OpenAiNotConfigured",
                        "OPENAI_API_KEY is not configured"
                ));
            }

            var bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("file", new FileSystemResource(filePath));
            bodyBuilder.part("model", aiProperties.whisper().model());
            bodyBuilder.part("response_format", "verbose_json");
            if ("whisper-1".equalsIgnoreCase(aiProperties.whisper().model())) {
                bodyBuilder.part("timestamp_granularities[]", "segment");
                bodyBuilder.part("timestamp_granularities[]", "word");
            }
            if (language != null) {
                bodyBuilder.part("language", language);
            }

            String whisperUrl = aiProperties.baseUrl() + "/audio/transcriptions";

            var response = restClient.post()
                    .uri(whisperUrl)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(bodyBuilder.build())
                    .retrieve()
                    .body(WhisperResponse.class);

            if (response == null) {
                return ResultT.failure(Error.failure("Transcription.Empty", "Whisper returned empty response"));
            }

            List<Segment> segments = List.of();
            if (response.segments != null) {
                segments = response.segments.stream()
                        .flatMap(s -> splitSegment(s).stream())
                        .toList();
            }

            List<TranscriptionService.Word> words = List.of();
            if (response.words != null) {
                words = response.words.stream()
                        .map(word -> new TranscriptionService.Word(
                                word.word,
                                word.start,
                                word.end,
                                word.probability != null ? word.probability : 0.9))
                        .toList();
            }

            var result = new TranscriptionResult(
                    response.text,
                    segments,
                    words,
                    response.language != null ? response.language : (language != null ? language : "en"),
                    response.duration != null ? response.duration : 0.0,
                    objectMapper.writeValueAsString(response)
            );

            log.info("Whisper transcription completed: {} segments, {} words, {}s duration, language={}",
                    segments.size(), words.size(), result.durationSeconds(), result.detectedLanguage());

            return ResultT.success(result);

        } catch (Exception e) {
            log.error("Whisper transcription failed for: {}", filePath.getFileName(), e);
            return ResultT.failure(Error.failure("Transcription.Failed",
                    "Whisper transcription failed: " + summarizeException(e)));
        }
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

    private List<Segment> splitSegment(WhisperSegment segment) {
        String text = segment.text == null ? "" : segment.text.trim();
        if (text.isBlank()) {
            return List.of();
        }

        double confidence = segment.avgLogprob != null ? Math.exp(segment.avgLogprob) : 0.9;
        double start = segment.start;
        double end = Math.max(segment.end, start + 0.1);
        double duration = Math.max(0.1, end - start);

        List<String> chunks = splitTextIntoChunks(text);
        if (chunks.size() == 1) {
            return List.of(new Segment(text, start, end, confidence));
        }

        int totalWeight = chunks.stream().mapToInt(this::wordCount).sum();
        if (totalWeight <= 0) {
            totalWeight = chunks.stream().mapToInt(String::length).sum();
        }
        if (totalWeight <= 0) {
            totalWeight = chunks.size();
        }

        List<Segment> result = new ArrayList<>();
        double cursor = start;
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i).trim();
            int weight = wordCount(chunk);
            if (weight <= 0) {
                weight = Math.max(1, chunk.length());
            }

            double chunkDuration = i == chunks.size() - 1
                    ? end - cursor
                    : duration * ((double) weight / totalWeight);
            double chunkEnd = Math.min(end, cursor + Math.max(0.1, chunkDuration));
            result.add(new Segment(chunk, cursor, chunkEnd, confidence));
            cursor = chunkEnd;
        }

        return result;
    }

    private List<String> splitTextIntoChunks(String text) {
        String[] rawParts = text.split("(?<=[.!?;:])\\s+|\\n+");
        List<String> chunks = new ArrayList<>();
        for (String part : rawParts) {
            String trimmed = part.trim();
            if (!trimmed.isBlank()) {
                chunks.add(trimmed);
            }
        }

        if (chunks.isEmpty()) {
            chunks.add(text);
        }

        if (chunks.size() == 1 && wordCount(text) > 12) {
            return splitByWordCount(text, 8);
        }

        if (chunks.size() == 1 && text.length() > 70) {
            return splitByWordCount(text, 8);
        }

        return chunks;
    }

    private List<String> splitByWordCount(String text, int wordsPerChunk) {
        String[] words = text.trim().split("\\s+");
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < words.length; i += wordsPerChunk) {
            int end = Math.min(words.length, i + wordsPerChunk);
            chunks.add(String.join(" ", java.util.Arrays.copyOfRange(words, i, end)));
        }
        return chunks;
    }

    private int wordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class WhisperResponse {
        public String text;
        public String language;
        public Double duration;
        public List<WhisperSegment> segments;
        public List<WhisperWord> words;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class WhisperSegment {
        public String text;
        public double start;
        public double end;
        @JsonProperty("avg_logprob")
        public Double avgLogprob;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class WhisperWord {
        public String word;
        public double start;
        public double end;
        @JsonProperty("probability")
        public Double probability;
    }
}
