package com.pigs.voxly.api.sessions;

import com.pigs.voxly.api.shared.ApiResponse;
import com.pigs.voxly.api.shared.ResultMapper;
import com.pigs.voxly.application.sessions.SessionService;
import com.pigs.voxly.application.sessions.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/v1/sessions")
@Tag(name = "Sessions", description = "Practice session management")
@SecurityRequirement(name = "bearerAuth")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    @Operation(summary = "Create a new practice session")
    public ResponseEntity<ApiResponse<SessionResponse>> createSession(
            @Valid @RequestBody CreateSessionRequest request
    ) {
        return ResultMapper.toCreatedResponse(sessionService.createSession(request));
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
        return ResultMapper.toResponse(sessionService.requestAnalysis(sessionId));
    }
}
