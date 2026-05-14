package com.pigs.voxly.infrastructure.evaluation;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pigs.voxly.application.shared.ports.PostureAnalysisService;
import com.pigs.voxly.sharedKernel.domain.results.Error;
import com.pigs.voxly.sharedKernel.domain.results.ResultT;

@Service
@ConditionalOnProperty(name = "posture.analyzer.enabled", havingValue = "true")
public class HttpPostureAnalysisService implements PostureAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(HttpPostureAnalysisService.class);

    private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration POLL_TIMEOUT = Duration.ofMinutes(15);

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String renderBase;

    public HttpPostureAnalysisService(ObjectMapper objectMapper, PostureAnalyzerProperties properties) {
        this.objectMapper = objectMapper;
        this.renderBase = properties.renderBase() != null ? properties.renderBase() : properties.url();
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(REQUEST_TIMEOUT);
        this.restClient = RestClient.builder()
                .baseUrl(properties.url())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public ResultT<PostureAnalysisResult> analyze(Path videoPath, Consumer<Double> progressCallback) {
        log.info("Posture analysis via HTTP (async) for: {}", videoPath.getFileName());

        try {
            var bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("video", new FileSystemResource(videoPath));
            bodyBuilder.part("render", "true");
            bodyBuilder.part("max_persons", "1");

            String submitJson = restClient.post()
                    .uri("/analyze/async")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(bodyBuilder.build())
                    .retrieve()
                    .body(String.class);

            if (submitJson == null || submitJson.isBlank()) {
                return ResultT.failure(Error.failure("POSTURE.EMPTY_RESPONSE", "Empty response from posture analyzer"));
            }

            JsonNode submitNode = objectMapper.readTree(submitJson);
            String jobId = submitNode.path("job_id").asText();
            if (jobId == null || jobId.isBlank()) {
                return ResultT.failure(Error.failure("POSTURE.NO_JOB_ID", "No job_id in async submission response"));
            }

            log.info("Posture analysis job started: {}", jobId);

            var deadline = System.currentTimeMillis() + POLL_TIMEOUT.toMillis();
            while (System.currentTimeMillis() < deadline) {
                Thread.sleep(POLL_INTERVAL.toMillis());

                String jobJson = restClient.get()
                        .uri("/jobs/{jobId}", jobId)
                        .retrieve()
                        .body(String.class);

                if (jobJson == null || jobJson.isBlank()) {
                    continue;
                }

                JsonNode jobNode = objectMapper.readTree(jobJson);
                String status = jobNode.path("status").asText();
                double progress = jobNode.path("progress").asDouble(0.0);

                progressCallback.accept(progress);

                switch (status) {
                    case "done":
                        log.info("Posture analysis job {} completed", jobId);
                        return ResultT.success(parseResult(jobNode));
                    case "error":
                        String errorMsg = jobNode.path("error").asText("Unknown error");
                        log.error("Posture analysis job {} failed: {}", jobId, errorMsg);
                        return ResultT.failure(Error.failure("POSTURE.JOB_ERROR", errorMsg));
                    case "pending":
                    case "processing":
                        log.debug("Posture job {} status={} progress={}", jobId, status, progress);
                        break;
                    default:
                        log.warn("Unknown job status for {}: {}", jobId, status);
                }
            }

            return ResultT.failure(Error.failure("POSTURE.TIMEOUT", "Posture analysis timed out after " + POLL_TIMEOUT.toMinutes() + " minutes"));

        } catch (RestClientException e) {
            log.error("Posture analyzer HTTP call failed", e);
            return ResultT.failure(Error.failure("POSTURE.HTTP_ERROR", e.getMessage()));
        } catch (IOException e) {
            log.error("Failed to parse posture analyzer response", e);
            return ResultT.failure(Error.failure("POSTURE.PARSE_ERROR", e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResultT.failure(Error.failure("POSTURE.INTERRUPTED", "Posture analysis was interrupted"));
        }
    }

    private PostureAnalysisResult parseResult(JsonNode jobNode) throws IOException {
        JsonNode analysisNode = jobNode.hasNonNull("analysis") ? jobNode.path("analysis") : jobNode;

        double finalScore = analysisNode.path("final_score").asDouble(100.0);
        int maxScore = analysisNode.path("max_score").asInt(100);
        String grade = analysisNode.path("grade").asText("A");

        String gestureSummariesJson = objectMapper.writeValueAsString(analysisNode.path("gesture_summaries"));
        String penaltyBreakdownJson = objectMapper.writeValueAsString(analysisNode.path("penalty_breakdown"));

        String timelineJson = objectMapper.writeValueAsString(analysisNode.path("timeline"));
        JsonNode recommendationsNode = analysisNode.path("recommendations");
        List<String> recommendations = objectMapper.convertValue(
                recommendationsNode,
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));

        String renderedVideoUrl = null;
        JsonNode metadataNode = jobNode.hasNonNull("metadata") ? jobNode.path("metadata") : null;
        if (metadataNode != null && metadataNode.hasNonNull("rendered_video_url")) {
            String relPath = metadataNode.path("rendered_video_url").asText();
            renderedVideoUrl = renderBase + relPath;
        }

        return new PostureAnalysisResult(finalScore, maxScore, grade, gestureSummariesJson,
                penaltyBreakdownJson, timelineJson, recommendations, renderedVideoUrl);
    }
}
