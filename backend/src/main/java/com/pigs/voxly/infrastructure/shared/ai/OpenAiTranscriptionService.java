package com.pigs.voxly.infrastructure.shared.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAiTranscriptionService implements TranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiTranscriptionService.class);
    private final RestClient restClient;
    private final AiProperties aiProperties;

    public OpenAiTranscriptionService(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
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
            bodyBuilder.part("timestamp_granularities[]", "segment");
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
                        .map(s -> new Segment(
                                s.text.trim(),
                                s.start,
                                s.end,
                                s.avgLogprob != null ? Math.exp(s.avgLogprob) : 0.9
                        ))
                        .toList();
            }

            var result = new TranscriptionResult(
                    response.text,
                    segments,
                    response.language != null ? response.language : (language != null ? language : "en"),
                    response.duration != null ? response.duration : 0.0
            );

            log.info("Whisper transcription completed: {} segments, {}s duration, language={}",
                    segments.size(), result.durationSeconds(), result.detectedLanguage());

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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class WhisperResponse {
        public String text;
        public String language;
        public Double duration;
        public List<WhisperSegment> segments;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class WhisperSegment {
        public String text;
        public double start;
        public double end;
        @JsonProperty("avg_logprob")
        public Double avgLogprob;
    }
}
