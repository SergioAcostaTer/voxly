package com.pigs.voxly.api.sessions;

import com.pigs.voxly.api.shared.ApiResponse;
import com.pigs.voxly.api.shared.ResultMapper;
import com.pigs.voxly.application.identity.ports.CurrentUserProvider;
import com.pigs.voxly.application.sessions.SessionService;
import com.pigs.voxly.application.sessions.SessionStatusStreamService;
import com.pigs.voxly.application.sessions.dto.*;
import com.pigs.voxly.infrastructure.shared.rateLimit.AnalysisRateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/v1/sessions")
@Tag(name = "Sessions", description = "Practice session management")
@SecurityRequirement(name = "bearerAuth")
public class SessionController {

    private final SessionService sessionService;
    private final CurrentUserProvider currentUserProvider;
    private final AnalysisRateLimiter analysisRateLimiter;
    private final SessionStatusStreamService sessionStatusStreamService;

    public SessionController(
            SessionService sessionService,
            CurrentUserProvider currentUserProvider,
            AnalysisRateLimiter analysisRateLimiter,
            SessionStatusStreamService sessionStatusStreamService) {
        this.sessionService = sessionService;
        this.currentUserProvider = currentUserProvider;
        this.analysisRateLimiter = analysisRateLimiter;
        this.sessionStatusStreamService = sessionStatusStreamService;
    }

    @PostMapping
    @Operation(summary = "Create a new practice session")
    public ResponseEntity<ApiResponse<SessionResponse>> createSession(
            @Valid @RequestBody CreateSessionRequest request
    ) {
        return ResultMapper.toCreatedResponse(sessionService.createSession(request));
    }

    @PostMapping(value = "/with-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a new practice session and upload media atomically")
    public ResponseEntity<ApiResponse<SessionResponse>> createSessionWithMedia(
            @RequestParam("title") String title,
            @RequestParam("sessionType") String sessionType,
            @RequestParam("language") String language,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("file") MultipartFile file
    ) {
        return ResultMapper.toCreatedResponse(sessionService.createSessionWithMedia(
                new CreateSessionRequest(title, description, sessionType, language),
                file));
    }

    @PostMapping("/recording-uploads")
    @Operation(summary = "Start a chunked recording upload")
    public ResponseEntity<ApiResponse<RecordingUploadResponse>> createRecordingUpload(
            @RequestParam("fileName") String fileName,
            @RequestParam("contentType") String contentType
    ) throws IOException {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.createRecordingUpload(fileName, contentType)));
    }

    @PostMapping(value = "/recording-uploads/{uploadId}/chunks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Append a chunk to a recording upload")
    public ResponseEntity<ApiResponse<RecordingUploadResponse>> appendRecordingChunk(
            @PathVariable UUID uploadId,
            @RequestParam("sequence") int sequence,
            @RequestParam(value = "isLastChunk", defaultValue = "false") boolean isLastChunk,
            @RequestParam("chunk") MultipartFile chunk
    ) throws IOException {
        return ResponseEntity.ok(ApiResponse.ok(
                sessionService.appendRecordingChunk(uploadId, chunk, sequence, isLastChunk)
        ));
    }

    @PostMapping("/with-recording-upload")
    @Operation(summary = "Create a new practice session from a streamed recording upload")
    public ResponseEntity<ApiResponse<SessionResponse>> createSessionWithRecordingUpload(
            @RequestParam("title") String title,
            @RequestParam("sessionType") String sessionType,
            @RequestParam("language") String language,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("uploadId") UUID uploadId
    ) {
        return ResultMapper.toCreatedResponse(sessionService.createSessionWithRecordingUpload(
                new CreateSessionRequest(title, description, sessionType, language),
                uploadId));
    }

    @DeleteMapping("/recording-uploads/{uploadId}")
    @Operation(summary = "Delete an unfinished recording upload")
    public ResponseEntity<ApiResponse<Void>> deleteRecordingUpload(@PathVariable UUID uploadId) {
        sessionService.deleteRecordingUpload(uploadId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @GetMapping
    @Operation(summary = "Get all sessions for the current user")
    public ResponseEntity<ApiResponse<SessionListResponse>> getUserSessions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResultMapper.toResponse(sessionService.getUserSessions(page, size));
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Get a session by ID")
    public ResponseEntity<ApiResponse<SessionResponse>> getSession(@PathVariable UUID sessionId) {
        return ResultMapper.toResponse(sessionService.getSession(sessionId));
    }

    @GetMapping(value = "/{sessionId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream live session and evaluation status updates")
    public SseEmitter streamSessionEvents(@PathVariable UUID sessionId) {
        var userId = currentUserProvider.getUserId()
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED));
        var sessionResult = sessionService.getSession(sessionId);
        if (sessionResult.isFailure()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Session not found");
        }
        return sessionStatusStreamService.subscribe(userId, sessionId);
    }

    @PatchMapping("/{sessionId}")
    @Operation(summary = "Update session details")
    public ResponseEntity<ApiResponse<SessionResponse>> updateSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody UpdateSessionRequest request
    ) {
        return ResultMapper.toResponse(sessionService.updateSession(sessionId, request));
    }

    @PostMapping(value = "/{sessionId}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload media file for a session")
    public ResponseEntity<ApiResponse<SessionResponse>> uploadMedia(
            @PathVariable UUID sessionId,
            @RequestParam("file") MultipartFile file
    ) {
        return ResultMapper.toResponse(sessionService.uploadMedia(sessionId, file));
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Delete a session")
    public ResponseEntity<ApiResponse<Void>> deleteSession(@PathVariable UUID sessionId) {
        return ResultMapper.toResponse(sessionService.deleteSession(sessionId));
    }

    @PostMapping("/{sessionId}/analyze")
    @Operation(summary = "Request AI analysis for a session")
    public ResponseEntity<ApiResponse<Void>> requestAnalysis(@PathVariable UUID sessionId) {
        currentUserProvider.getUserId().ifPresent(analysisRateLimiter::checkAllowed);
        return ResultMapper.toResponse(sessionService.requestAnalysis(sessionId));
    }
}
