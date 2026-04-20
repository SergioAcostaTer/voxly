package com.pigs.voxly.api.evaluation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.pigs.voxly.api.shared.ApiResponse;
import com.pigs.voxly.api.shared.ResultMapper;
import com.pigs.voxly.application.evaluation.EvaluationService;
import com.pigs.voxly.application.evaluation.TranscriptionService;
import com.pigs.voxly.application.evaluation.dto.EvaluationResponse;
import com.pigs.voxly.domain.evaluation.Transcription;
import com.pigs.voxly.infrastructure.shared.rateLimit.AnalysisRateLimiter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/evaluations")
@Tag(name = "Evaluations", description = "AI-powered presentation analysis")
@SecurityRequirement(name = "bearerAuth")
public class EvaluationController {

    private static final Logger logger = LoggerFactory.getLogger(EvaluationController.class);
    private static final long MAX_VIDEO_SIZE = 500 * 1024 * 1024; // 500MB

    private final EvaluationService evaluationService;
    private final TranscriptionService transcriptionService;
    private final AnalysisRateLimiter analysisRateLimiter;

    public EvaluationController(
            EvaluationService evaluationService,
            TranscriptionService transcriptionService,
            AnalysisRateLimiter analysisRateLimiter) {
        this.evaluationService = evaluationService;
        this.transcriptionService = transcriptionService;
        this.analysisRateLimiter = analysisRateLimiter;
    }

    @GetMapping("/{evaluationId}")
    @Operation(summary = "Get an evaluation by ID")
    public ResponseEntity<ApiResponse<EvaluationResponse>> getEvaluation(@PathVariable UUID evaluationId) {
        return ResultMapper.toResponse(evaluationService.getEvaluation(evaluationId));
    }

    @GetMapping("/session/{sessionId}")
    @Operation(summary = "Get evaluation for a specific session")
    public ResponseEntity<ApiResponse<EvaluationResponse>> getEvaluationBySession(@PathVariable UUID sessionId) {
        return ResultMapper.toResponse(evaluationService.getEvaluationBySessionId(sessionId));
    }

    @PostMapping("/session/{sessionId}")
    @Operation(summary = "Start AI analysis for a session")
    public ResponseEntity<ApiResponse<EvaluationResponse>> startEvaluation(@PathVariable UUID sessionId) {
        return ResultMapper.toCreatedResponse(evaluationService.startEvaluation(sessionId));
    }

    @PostMapping("/{sessionId}/transcribe")
    @Operation(summary = "Request transcription for session video")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Transcription request accepted", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "413", description = "File too large")
    })
    public ResponseEntity<ApiResponse<TranscriptionResponse>> requestTranscription(
            @PathVariable UUID sessionId,
            @RequestParam @Parameter(description = "Video file (max 500MB)") MultipartFile file,
            Authentication authentication) {

        Path tempFile = null;
        boolean handoffSucceeded = false;

        try {
            UUID userId = UUID.fromString(authentication.getName());
            analysisRateLimiter.checkAllowed(userId);

            // Validate file
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("INVALID_FILE", "Video file is required"));
            }

            if (file.getSize() > MAX_VIDEO_SIZE) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
                        ApiResponse.error("FILE_TOO_LARGE", "File exceeds maximum size of 500MB"));
            }

            // Save temp file
            tempFile = Files.createTempFile("voxly_video_", ".mp4");
            file.transferTo(tempFile);

            logger.info("Transcription requested for session: {}, file size: {} MB",
                    sessionId, file.getSize() / (1024 * 1024));

            // Request transcription
            Transcription transcription = transcriptionService.requestTranscription(
                    sessionId, userId, tempFile.toFile());
            handoffSucceeded = true;

            return ResponseEntity.accepted().body(
                    ApiResponse.ok(new TranscriptionResponse(transcription)));

        } catch (IOException e) {
            logger.error("File upload failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("UPLOAD_ERROR", "File upload failed"));
        } finally {
            if (!handoffSucceeded && tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    logger.warn("Failed to delete failed transcription upload temp file: {}", tempFile, e);
                }
            }
        }
    }

    @GetMapping("/{sessionId}/transcription")
    @Operation(summary = "Get transcription for session")
    public ResponseEntity<ApiResponse<TranscriptionResponse>> getTranscription(
            @PathVariable UUID sessionId,
            Authentication authentication) {

        try {
            UUID userId = UUID.fromString(authentication.getName());

            Optional<Transcription> transcription = transcriptionService.getTranscription(sessionId, userId);

            if (transcription.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error("NOT_FOUND", "Transcription not found"));
            }

            return ResponseEntity.ok(ApiResponse.ok(
                    new TranscriptionResponse(transcription.get())));

        } catch (Exception e) {
            logger.error("Failed to get transcription: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("INTERNAL_ERROR", "Failed to retrieve transcription"));
        }
    }

    // DTO
    public static class TranscriptionResponse {
        public UUID id;
        public String status;
        public String originalText;
        public Integer durationSeconds;
        public Integer wordCount;
        public String language;
        public String errorMessage;

        public TranscriptionResponse(Transcription transcription) {
            this.id = transcription.getId();
            this.status = transcription.getStatus();
            this.originalText = transcription.getOriginalText();
            this.durationSeconds = transcription.getDurationSeconds();
            this.wordCount = transcription.getWordCount();
            this.language = transcription.getLanguage();
            this.errorMessage = transcription.getErrorMessage();
        }
    }
}
