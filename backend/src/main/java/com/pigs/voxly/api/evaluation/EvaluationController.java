package com.pigs.voxly.api.evaluation;

import com.pigs.voxly.api.shared.ApiResponse;
import com.pigs.voxly.api.shared.ResultMapper;
import com.pigs.voxly.application.evaluation.EvaluationService;
import com.pigs.voxly.application.evaluation.dto.EvaluationResponse;
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

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
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
}
