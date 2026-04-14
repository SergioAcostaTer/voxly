package com.pigs.voxly.api.feedback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pigs.voxly.api.shared.ApiResponse;
import com.pigs.voxly.application.evaluation.EvaluationService;
import com.pigs.voxly.application.shared.ports.SpeechAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/feedback")
@Tag(name = "Feedback", description = "Timestamped presentation feedback")
@SecurityRequirement(name = "bearerAuth")
public class FeedbackController {

    private final EvaluationService evaluationService;
    private final ObjectMapper objectMapper;

    public FeedbackController(EvaluationService evaluationService, ObjectMapper objectMapper) {
        this.evaluationService = evaluationService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/session/{sessionId}")
    @Operation(summary = "Get all feedback notes for a session")
    public ResponseEntity<ApiResponse<FeedbackResponse>> getFeedbackForSession(@PathVariable UUID sessionId) {
        var evalResult = evaluationService.getEvaluationBySessionId(sessionId);

        if (evalResult.isFailure()) {
            return ResponseEntity.notFound().build();
        }

        var evaluation = evalResult.getValue();

        if (evaluation.feedback() == null) {
            return ResponseEntity.ok(ApiResponse.ok(new FeedbackResponse(
                    sessionId,
                    evaluation.id(),
                    Collections.emptyList(),
                    null,
                    Collections.emptyList(),
                    Collections.emptyList()
            )));
        }

        try {
            List<SpeechAnalysisService.FeedbackNote> notes = objectMapper.readValue(
                    evaluation.feedback().notesJson(),
                    new TypeReference<>() {}
            );

            List<String> strengths = evaluation.metrics() != null ?
                    parseJsonList(evaluation.feedback().overallSummary()) : Collections.emptyList();

            return ResponseEntity.ok(ApiResponse.ok(new FeedbackResponse(
                    sessionId,
                    evaluation.id(),
                    notes,
                    evaluation.feedback().overallSummary(),
                    Collections.emptyList(), // Will be populated from evaluation
                    Collections.emptyList()
            )));

        } catch (JsonProcessingException e) {
            return ResponseEntity.ok(ApiResponse.ok(new FeedbackResponse(
                    sessionId,
                    evaluation.id(),
                    Collections.emptyList(),
                    evaluation.feedback().overallSummary(),
                    Collections.emptyList(),
                    Collections.emptyList()
            )));
        }
    }

    @GetMapping("/session/{sessionId}/category/{category}")
    @Operation(summary = "Get feedback notes filtered by category")
    public ResponseEntity<ApiResponse<List<SpeechAnalysisService.FeedbackNote>>> getFeedbackByCategory(
            @PathVariable UUID sessionId,
            @PathVariable String category
    ) {
        var evalResult = evaluationService.getEvaluationBySessionId(sessionId);

        if (evalResult.isFailure()) {
            return ResponseEntity.notFound().build();
        }

        var evaluation = evalResult.getValue();

        if (evaluation.feedback() == null || evaluation.feedback().notesJson() == null) {
            return ResponseEntity.ok(ApiResponse.ok(Collections.emptyList()));
        }

        try {
            List<SpeechAnalysisService.FeedbackNote> allNotes = objectMapper.readValue(
                    evaluation.feedback().notesJson(),
                    new TypeReference<>() {}
            );

            var filteredNotes = allNotes.stream()
                    .filter(note -> note.category().equalsIgnoreCase(category))
                    .toList();

            return ResponseEntity.ok(ApiResponse.ok(filteredNotes));

        } catch (JsonProcessingException e) {
            return ResponseEntity.ok(ApiResponse.ok(Collections.emptyList()));
        }
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    public record FeedbackResponse(
            UUID sessionId,
            UUID evaluationId,
            List<SpeechAnalysisService.FeedbackNote> notes,
            String overallSummary,
            List<String> strengths,
            List<String> areasForImprovement
    ) {}
}
