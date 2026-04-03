package com.pigs.voxly.api.progress;

import com.pigs.voxly.api.shared.ApiResponse;
import com.pigs.voxly.application.identity.ports.CurrentUserProvider;
import com.pigs.voxly.domain.evaluation.EvaluationRepository;
import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.domain.sessions.SessionRepository;
import com.pigs.voxly.domain.sessions.enumerations.SessionStatus;
import com.pigs.voxly.infrastructure.shared.dev.DevMockDataService;
import com.pigs.voxly.sharedKernel.domain.pagination.PagedRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/v1/progress")
@Tag(name = "Progress", description = "User progress tracking and analytics")
@SecurityRequirement(name = "bearerAuth")
public class ProgressController {

    private final SessionRepository sessionRepository;
    private final EvaluationRepository evaluationRepository;
    private final CurrentUserProvider currentUserProvider;
    private final DevMockDataService devMockDataService;

    public ProgressController(
            SessionRepository sessionRepository,
            EvaluationRepository evaluationRepository,
            CurrentUserProvider currentUserProvider,
            DevMockDataService devMockDataService
    ) {
        this.sessionRepository = sessionRepository;
        this.evaluationRepository = evaluationRepository;
        this.currentUserProvider = currentUserProvider;
        this.devMockDataService = devMockDataService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get user's progress summary")
    public ResponseEntity<ApiResponse<ProgressSummary>> getProgressSummary() {
        if (devMockDataService.isDevUser()) {
            return ResponseEntity.ok(ApiResponse.ok(devMockDataService.getMockProgressSummary()));
        }

        var userIdOpt = currentUserProvider.getUserId();
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        var userId = UserId.from(userIdOpt.get());
        var totalSessions = sessionRepository.countByUserId(userId);

        // Get completed sessions with evaluations
        var sessions = sessionRepository.findByUserId(userId, PagedRequest.of(1, 100));

        long completedSessions = sessions.getItems().stream()
                .filter(s -> s.getStatus() == SessionStatus.COMPLETED)
                .count();

        // Calculate average metrics from completed evaluations
        List<Double> clarityScores = new ArrayList<>();
        List<Integer> wpmValues = new ArrayList<>();
        List<Integer> fillerCounts = new ArrayList<>();

        for (var session : sessions.getItems()) {
            if (session.getEvaluationId() != null) {
                var evalOpt = evaluationRepository.findBySessionId(session.getId());
                evalOpt.ifPresent(eval -> {
                    if (eval.getClarityScore() != null) clarityScores.add(eval.getClarityScore());
                    if (eval.getWordsPerMinute() != null) wpmValues.add(eval.getWordsPerMinute());
                    if (eval.getFillerWordCount() != null) fillerCounts.add(eval.getFillerWordCount());
                });
            }
        }

        Double avgClarity = clarityScores.isEmpty() ? null :
                clarityScores.stream().mapToDouble(d -> d).average().orElse(0);
        Integer avgWpm = wpmValues.isEmpty() ? null :
                (int) wpmValues.stream().mapToInt(i -> i).average().orElse(0);
        Integer avgFillers = fillerCounts.isEmpty() ? null :
                (int) fillerCounts.stream().mapToInt(i -> i).average().orElse(0);

        return ResponseEntity.ok(ApiResponse.ok(new ProgressSummary(
                totalSessions,
                completedSessions,
                avgClarity,
                avgWpm,
                avgFillers,
                sessions.getItems().size()
        )));
    }

    @GetMapping("/trends")
    @Operation(summary = "Get user's progress trends over time")
    public ResponseEntity<ApiResponse<List<SessionTrend>>> getProgressTrends(
            @RequestParam(defaultValue = "10") int limit
    ) {
        if (devMockDataService.isDevUser()) {
            return ResponseEntity.ok(ApiResponse.ok(devMockDataService.getMockProgressTrends()));
        }

        var userIdOpt = currentUserProvider.getUserId();
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        var userId = UserId.from(userIdOpt.get());
        var sessions = sessionRepository.findByUserId(userId, PagedRequest.of(1, limit));

        List<SessionTrend> trends = new ArrayList<>();

        for (var session : sessions.getItems()) {
            if (session.getStatus() == SessionStatus.COMPLETED && session.getEvaluationId() != null) {
                var evalOpt = evaluationRepository.findBySessionId(session.getId());
                evalOpt.ifPresent(eval -> {
                    trends.add(new SessionTrend(
                            session.getId().getValue(),
                            session.getTitle().getValue(),
                            session.getCreatedAt(),
                            eval.getClarityScore(),
                            eval.getWordsPerMinute(),
                            eval.getFillerWordCount()
                    ));
                });
            }
        }

        return ResponseEntity.ok(ApiResponse.ok(trends));
    }

    public record ProgressSummary(
            long totalSessions,
            long completedSessions,
            Double averageClarityScore,
            Integer averageWordsPerMinute,
            Integer averageFillerWords,
            int recentSessionCount
    ) {}

    public record SessionTrend(
            java.util.UUID sessionId,
            String sessionTitle,
            java.time.Instant date,
            Double clarityScore,
            Integer wordsPerMinute,
            Integer fillerWordCount
    ) {}
}
