package com.pigs.voxly.api.evaluation;

import com.pigs.voxly.api.shared.ApiResponse;
import com.pigs.voxly.api.shared.ResultMapper;
import com.pigs.voxly.application.evaluation.EvaluationService;
import com.pigs.voxly.application.evaluation.dto.EvaluationResponse;
import com.pigs.voxly.infrastructure.shared.dev.DevMockDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/evaluations")
@Tag(name = "Evaluations", description = "AI-powered presentation analysis")
@SecurityRequirement(name = "bearerAuth")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final DevMockDataService devMockDataService;

    public EvaluationController(EvaluationService evaluationService, DevMockDataService devMockDataService) {
        this.evaluationService = evaluationService;
        this.devMockDataService = devMockDataService;
    }

    @GetMapping("/{evaluationId}")
    @Operation(summary = "Get an evaluation by ID")
    public ResponseEntity<ApiResponse<EvaluationResponse>> getEvaluation(@PathVariable UUID evaluationId) {
        if (devMockDataService.isDevUser()) {
            // Return mock evaluation for session 1 as default
            return ResponseEntity.ok(ApiResponse.ok(devMockDataService.getMockEvaluation(
                    UUID.fromString("11111111-1111-1111-1111-111111111111"))));
        }
        return ResultMapper.toResponse(evaluationService.getEvaluation(evaluationId));
    }

    @GetMapping("/session/{sessionId}")
    @Operation(summary = "Get evaluation for a specific session")
    public ResponseEntity<ApiResponse<EvaluationResponse>> getEvaluationBySession(@PathVariable UUID sessionId) {
        if (devMockDataService.isDevUser()) {
            return ResponseEntity.ok(ApiResponse.ok(devMockDataService.getMockEvaluation(sessionId)));
        }
        return ResultMapper.toResponse(evaluationService.getEvaluationBySessionId(sessionId));
    }

    @PostMapping("/session/{sessionId}")
    @Operation(summary = "Start AI analysis for a session")
    public ResponseEntity<ApiResponse<EvaluationResponse>> startEvaluation(@PathVariable UUID sessionId) {
        if (devMockDataService.isDevUser()) {
            return ResponseEntity.ok(ApiResponse.ok(devMockDataService.getMockEvaluation(sessionId)));
        }
        return ResultMapper.toCreatedResponse(evaluationService.startEvaluation(sessionId));
    }
}
