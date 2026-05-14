package com.pigs.voxly.api.progress;

import com.pigs.voxly.api.shared.ApiResponse;
import com.pigs.voxly.application.identity.ports.CurrentUserProvider;
import com.pigs.voxly.domain.evaluation.Evaluation;
import com.pigs.voxly.domain.evaluation.EvaluationRepository;
import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.domain.sessions.Session;
import com.pigs.voxly.domain.sessions.SessionRepository;
import com.pigs.voxly.domain.sessions.enumerations.SessionStatus;
import com.pigs.voxly.infrastructure.evaluation.PostureAnalyzerProperties;
import com.pigs.voxly.sharedKernel.domain.pagination.PagedRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/progress")
@Tag(name = "Progress", description = "User progress tracking and analytics")
@SecurityRequirement(name = "bearerAuth")
public class ProgressController {

    private final SessionRepository sessionRepository;
    private final EvaluationRepository evaluationRepository;
    private final CurrentUserProvider currentUserProvider;
    private final PostureAnalyzerProperties postureAnalyzerProperties;

    public ProgressController(
            SessionRepository sessionRepository,
            EvaluationRepository evaluationRepository,
            CurrentUserProvider currentUserProvider,
            PostureAnalyzerProperties postureAnalyzerProperties
    ) {
        this.sessionRepository = sessionRepository;
        this.evaluationRepository = evaluationRepository;
        this.currentUserProvider = currentUserProvider;
        this.postureAnalyzerProperties = postureAnalyzerProperties;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get user's progress summary")
    public ResponseEntity<ApiResponse<ProgressSummary>> getProgressSummary() {
        var userIdOpt = currentUserProvider.getUserId();
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        var userId = UserId.from(userIdOpt.get());
        var totalSessions = sessionRepository.countByUserId(userId);
        var sessions = sessionRepository.findByUserId(userId, PagedRequest.of(1, 100));

        long completedSessions = sessions.getItems().stream()
                .filter(s -> s.getStatus() == SessionStatus.COMPLETED)
                .count();
        long failedSessions = sessions.getItems().stream()
                .filter(s -> s.getStatus() == SessionStatus.FAILED)
                .count();
        long processingSessions = sessions.getItems().stream()
                .filter(s -> s.getStatus() == SessionStatus.UPLOADED || s.getStatus() == SessionStatus.ANALYZING)
                .count();

        List<Double> clarityScores = new ArrayList<>();
        List<Integer> wpmValues = new ArrayList<>();
        List<Integer> fillerCounts = new ArrayList<>();
        List<Double> postureScores = new ArrayList<>();
        long[] completedVideoAnalyses = {0};

        for (var session : sessions.getItems()) {
            if (session.getEvaluationId() == null) {
                continue;
            }

            var evalOpt = evaluationRepository.findBySessionId(session.getId());
            evalOpt.ifPresent(eval -> {
                if (eval.getClarityScore() != null) clarityScores.add(eval.getClarityScore());
                if (eval.getWordsPerMinute() != null) wpmValues.add(eval.getWordsPerMinute());
                if (eval.getFillerWordCount() != null) fillerCounts.add(eval.getFillerWordCount());
                if (eval.getPostureScore() != null) postureScores.add(eval.getPostureScore());
                if (eval.getPostureScore() != null || eval.getPostureGrade() != null) {
                    completedVideoAnalyses[0]++;
                }
            });
        }

        Double avgClarity = clarityScores.isEmpty() ? null :
                clarityScores.stream().mapToDouble(d -> d).average().orElse(0);
        Integer avgWpm = wpmValues.isEmpty() ? null :
                (int) wpmValues.stream().mapToInt(i -> i).average().orElse(0);
        Integer avgFillers = fillerCounts.isEmpty() ? null :
                (int) fillerCounts.stream().mapToInt(i -> i).average().orElse(0);
        Double avgPosture = postureScores.isEmpty() ? null :
                postureScores.stream().mapToDouble(d -> d).average().orElse(0);

        return ResponseEntity.ok(ApiResponse.ok(new ProgressSummary(
                totalSessions,
                completedSessions,
                avgClarity,
                avgWpm,
                avgFillers,
                sessions.getItems().size(),
                avgPosture,
                completedVideoAnalyses[0],
                processingSessions,
                failedSessions,
                postureAnalyzerProperties.enabled()
        )));
    }

    @GetMapping("/trends")
    @Operation(summary = "Get user's progress trends over time")
    public ResponseEntity<ApiResponse<List<SessionTrend>>> getProgressTrends(
            @RequestParam(defaultValue = "10") int limit
    ) {
        var userIdOpt = currentUserProvider.getUserId();
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        var records = loadCompletedRecords(UserId.from(userIdOpt.get()), limit);
        List<SessionTrend> trends = records.stream()
                .map(record -> new SessionTrend(
                        record.session().getId().getValue(),
                        record.session().getTitle().getValue(),
                        record.session().getCreatedAt(),
                        record.evaluation().getClarityScore(),
                        record.evaluation().getWordsPerMinute(),
                        record.evaluation().getFillerWordCount()
                ))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(trends));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get progress averages by metric category")
    public ResponseEntity<ApiResponse<List<CategoryProgress>>> getProgressByCategory() {
        var userIdOpt = currentUserProvider.getUserId();
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        var records = loadCompletedRecords(UserId.from(userIdOpt.get()), 100);
        List<CategoryProgress> categories = List.of(
                buildCategory("clarity", "Clarity Score", averageDouble(records, eval -> eval.getClarityScore()), 10.0, true),
                buildCategory("pace", "Words Per Minute", averageInt(records, eval -> eval.getWordsPerMinute()), 150.0, true),
                buildCategory("fillers", "Filler Words", averageInt(records, eval -> eval.getFillerWordCount()), 5.0, false),
                buildCategory("pauses", "Pause Count", averageInt(records, eval -> eval.getPauseCount()), 8.0, true),
                buildCategory("posture", "Body Language Score", averageDouble(records, eval -> eval.getPostureScore()), 100.0, true)
        );

        return ResponseEntity.ok(ApiResponse.ok(categories));
    }

    @GetMapping("/compare")
    @Operation(summary = "Compare two completed sessions")
    public ResponseEntity<ApiResponse<SessionComparison>> compareSessions(
            @RequestParam UUID firstSessionId,
            @RequestParam UUID secondSessionId
    ) {
        var userIdOpt = currentUserProvider.getUserId();
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        var userId = UserId.from(userIdOpt.get());
        var first = loadOwnedCompletedRecord(userId, firstSessionId);
        var second = loadOwnedCompletedRecord(userId, secondSessionId);
        if (first == null || second == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ApiResponse.ok(new SessionComparison(
                toSnapshot(first),
                toSnapshot(second),
                List.of(
                        compareMetric("Clarity", first.evaluation().getClarityScore(), second.evaluation().getClarityScore(), true),
                        compareMetric("Words Per Minute", first.evaluation().getWordsPerMinute(), second.evaluation().getWordsPerMinute(), true),
                        compareMetric("Filler Words", first.evaluation().getFillerWordCount(), second.evaluation().getFillerWordCount(), false),
                        compareMetric("Pause Count", first.evaluation().getPauseCount(), second.evaluation().getPauseCount(), false),
                        compareMetric("Body Language", first.evaluation().getPostureScore(), second.evaluation().getPostureScore(), true)
                )
        )));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(summary = "Export completed session progress as CSV")
    public ResponseEntity<byte[]> exportProgressSummary() {
        var userIdOpt = currentUserProvider.getUserId();
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        var records = loadCompletedRecords(UserId.from(userIdOpt.get()), 200);
        StringBuilder csv = new StringBuilder();
        csv.append("session_id,title,date,clarity_score,words_per_minute,filler_words,pause_count,posture_score,posture_grade\n");
        for (var record : records) {
            csv.append(record.session().getId().getValue()).append(',')
                    .append(escapeCsv(record.session().getTitle().getValue())).append(',')
                    .append(record.session().getCreatedAt()).append(',')
                    .append(formatCsv(record.evaluation().getClarityScore())).append(',')
                    .append(formatCsv(record.evaluation().getWordsPerMinute())).append(',')
                    .append(formatCsv(record.evaluation().getFillerWordCount())).append(',')
                    .append(formatCsv(record.evaluation().getPauseCount())).append(',')
                    .append(formatCsv(record.evaluation().getPostureScore())).append(',')
                    .append(formatCsv(record.evaluation().getPostureGrade()))
                    .append('\n');
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=voxly-progress.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private List<SessionEvaluationRecord> loadCompletedRecords(UserId userId, int limit) {
        var sessions = sessionRepository.findByUserId(userId, PagedRequest.of(1, limit));
        return sessions.getItems().stream()
                .filter(session -> session.getStatus() == SessionStatus.COMPLETED && session.getEvaluationId() != null)
                .map(session -> evaluationRepository.findBySessionId(session.getId())
                        .map(evaluation -> new SessionEvaluationRecord(session, evaluation))
                        .orElse(null))
                .filter(record -> record != null)
                .sorted(Comparator.comparing((SessionEvaluationRecord record) -> record.session().getCreatedAt()).reversed())
                .toList();
    }

    private SessionEvaluationRecord loadOwnedCompletedRecord(UserId userId, UUID sessionId) {
        var sessionOpt = sessionRepository.findByIdAndUserId(com.pigs.voxly.domain.sessions.SessionId.from(sessionId), userId);
        if (sessionOpt.isEmpty()) {
            return null;
        }

        var session = sessionOpt.get();
        if (session.getStatus() != SessionStatus.COMPLETED || session.getEvaluationId() == null) {
            return null;
        }

        return evaluationRepository.findBySessionId(session.getId())
                .map(evaluation -> new SessionEvaluationRecord(session, evaluation))
                .orElse(null);
    }

    private CategoryProgress buildCategory(String key, String label, Double averageValue, Double targetValue, boolean higherIsBetter) {
        String status = "no-data";
        if (averageValue != null) {
            boolean healthy = higherIsBetter ? averageValue >= targetValue * 0.8 : averageValue <= targetValue * 1.2;
            status = healthy ? "on-track" : "needs-work";
        }
        return new CategoryProgress(key, label, averageValue, targetValue, higherIsBetter, status);
    }

    private Double averageDouble(List<SessionEvaluationRecord> records, java.util.function.Function<Evaluation, Double> extractor) {
        var values = records.stream()
                .map(record -> extractor.apply(record.evaluation()))
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .toArray();
        if (values.length == 0) {
            return null;
        }
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    private Double averageInt(List<SessionEvaluationRecord> records, java.util.function.Function<Evaluation, Integer> extractor) {
        var values = records.stream()
                .map(record -> extractor.apply(record.evaluation()))
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .toArray();
        if (values.length == 0) {
            return null;
        }
        int sum = 0;
        for (int value : values) {
            sum += value;
        }
        return (double) sum / values.length;
    }

    private MetricComparison compareMetric(String label, Number firstValue, Number secondValue, boolean higherIsBetter) {
        Double first = firstValue == null ? null : firstValue.doubleValue();
        Double second = secondValue == null ? null : secondValue.doubleValue();
        Double delta = first == null || second == null ? null : second - first;
        String winner;
        if (delta == null || delta == 0) {
            winner = "tie";
        } else {
            boolean secondWins = higherIsBetter ? delta > 0 : delta < 0;
            winner = secondWins ? "second" : "first";
        }
        return new MetricComparison(label, first, second, delta, winner);
    }

    private SessionSnapshot toSnapshot(SessionEvaluationRecord record) {
        return new SessionSnapshot(
                record.session().getId().getValue(),
                record.session().getTitle().getValue(),
                record.session().getCreatedAt()
        );
    }

    private String escapeCsv(String value) {
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private String formatCsv(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record SessionEvaluationRecord(Session session, Evaluation evaluation) {}

    public record ProgressSummary(
            long totalSessions,
            long completedSessions,
            Double averageClarityScore,
            Integer averageWordsPerMinute,
            Integer averageFillerWords,
            int recentSessionCount,
            Double averagePostureScore,
            long completedVideoAnalyses,
            long processingSessions,
            long failedSessions,
            boolean aiVideoModuleEnabled
    ) {}

    public record SessionTrend(
            UUID sessionId,
            String sessionTitle,
            Instant date,
            Double clarityScore,
            Integer wordsPerMinute,
            Integer fillerWordCount
    ) {}

    public record CategoryProgress(
            String key,
            String label,
            Double averageValue,
            Double targetValue,
            boolean higherIsBetter,
            String status
    ) {}

    public record SessionComparison(
            SessionSnapshot firstSession,
            SessionSnapshot secondSession,
            List<MetricComparison> metrics
    ) {}

    public record SessionSnapshot(
            UUID sessionId,
            String title,
            Instant date
    ) {}

    public record MetricComparison(
            String label,
            Double firstValue,
            Double secondValue,
            Double delta,
            String winner
    ) {}
}
