package com.pigs.voxly.domain.evaluation;

import com.pigs.voxly.domain.evaluation.enumerations.EvaluationStatus;
import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.domain.sessions.SessionId;
import com.pigs.voxly.sharedKernel.domain.ddd.AggregateRoot;
import com.pigs.voxly.sharedKernel.domain.results.Result;
import com.pigs.voxly.sharedKernel.domain.results.ResultT;

import java.time.Instant;

public final class Evaluation extends AggregateRoot<EvaluationId> {

    private SessionId sessionId;
    private UserId userId;
    private EvaluationStatus status;
    private String sessionType;

    // Transcription data
    private String transcriptionText;
    private String transcriptionJson; // Full segments JSON
    private Double durationSeconds;
    private String detectedLanguage;

    // Metrics
    private Integer wordsPerMinute;
    private Integer totalWords;
    private Integer fillerWordCount;
    private Integer pauseCount;
    private Double clarityScore;
    private String metricsJson; // Full metrics JSON

    // Feedback
    private String feedbackJson; // Full feedback notes JSON
    private String overallSummary;
    private String strengthsJson;
    private String improvementsJson;

    // Error handling
    private String errorMessage;

    // Timestamps
    private Instant createdAt;
    private Instant completedAt;

    private Evaluation() {}

    private Evaluation(EvaluationId id, SessionId sessionId, UserId userId, String sessionType) {
        super(id);
        this.sessionId = sessionId;
        this.userId = userId;
        this.sessionType = sessionType;
        this.status = EvaluationStatus.PENDING;
        this.createdAt = Instant.now();
    }

    // ===== Factory =====

    public static ResultT<Evaluation> create(SessionId sessionId, UserId userId, String sessionType) {
        return ResultT.success(new Evaluation(EvaluationId.create(), sessionId, userId, sessionType));
    }

    // ===== Reconstitution =====

    public static Evaluation reconstitute(
            EvaluationId id,
            SessionId sessionId,
            UserId userId,
            EvaluationStatus status,
            String sessionType,
            String transcriptionText,
            String transcriptionJson,
            Double durationSeconds,
            String detectedLanguage,
            Integer wordsPerMinute,
            Integer totalWords,
            Integer fillerWordCount,
            Integer pauseCount,
            Double clarityScore,
            String metricsJson,
            String feedbackJson,
            String overallSummary,
            String strengthsJson,
            String improvementsJson,
            String errorMessage,
            Instant createdAt,
            Instant completedAt
    ) {
        var evaluation = new Evaluation();
        evaluation.id = id;
        evaluation.sessionId = sessionId;
        evaluation.userId = userId;
        evaluation.status = status;
        evaluation.sessionType = sessionType;
        evaluation.transcriptionText = transcriptionText;
        evaluation.transcriptionJson = transcriptionJson;
        evaluation.durationSeconds = durationSeconds;
        evaluation.detectedLanguage = detectedLanguage;
        evaluation.wordsPerMinute = wordsPerMinute;
        evaluation.totalWords = totalWords;
        evaluation.fillerWordCount = fillerWordCount;
        evaluation.pauseCount = pauseCount;
        evaluation.clarityScore = clarityScore;
        evaluation.metricsJson = metricsJson;
        evaluation.feedbackJson = feedbackJson;
        evaluation.overallSummary = overallSummary;
        evaluation.strengthsJson = strengthsJson;
        evaluation.improvementsJson = improvementsJson;
        evaluation.errorMessage = errorMessage;
        evaluation.createdAt = createdAt;
        evaluation.completedAt = completedAt;
        return evaluation;
    }

    // ===== Processing Operations =====

    public Result startTranscription() {
        if (status != EvaluationStatus.PENDING) {
            return Result.failure(EvaluationErrors.ALREADY_COMPLETED);
        }
        this.status = EvaluationStatus.TRANSCRIBING;
        return Result.success();
    }

    public Result completeTranscription(
            String fullText,
            String segmentsJson,
            double durationSeconds,
            String detectedLanguage
    ) {
        this.transcriptionText = fullText;
        this.transcriptionJson = segmentsJson;
        this.durationSeconds = durationSeconds;
        this.detectedLanguage = detectedLanguage;
        this.status = EvaluationStatus.ANALYZING;
        return Result.success();
    }

    public Result completeAnalysis(
            int wordsPerMinute,
            int totalWords,
            int fillerWordCount,
            int pauseCount,
            double clarityScore,
            String metricsJson,
            String feedbackJson,
            String overallSummary,
            String strengthsJson,
            String improvementsJson
    ) {
        this.wordsPerMinute = wordsPerMinute;
        this.totalWords = totalWords;
        this.fillerWordCount = fillerWordCount;
        this.pauseCount = pauseCount;
        this.clarityScore = clarityScore;
        this.metricsJson = metricsJson;
        this.feedbackJson = feedbackJson;
        this.overallSummary = overallSummary;
        this.strengthsJson = strengthsJson;
        this.improvementsJson = improvementsJson;
        this.status = EvaluationStatus.COMPLETED;
        this.completedAt = Instant.now();
        return Result.success();
    }

    public Result fail(String errorMessage) {
        this.status = EvaluationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
        return Result.success();
    }

    // ===== Getters =====

    public SessionId getSessionId() {
        return sessionId;
    }

    public UserId getUserId() {
        return userId;
    }

    public EvaluationStatus getStatus() {
        return status;
    }

    public String getSessionType() {
        return sessionType;
    }

    public String getTranscriptionText() {
        return transcriptionText;
    }

    public String getTranscriptionJson() {
        return transcriptionJson;
    }

    public Double getDurationSeconds() {
        return durationSeconds;
    }

    public String getDetectedLanguage() {
        return detectedLanguage;
    }

    public Integer getWordsPerMinute() {
        return wordsPerMinute;
    }

    public Integer getTotalWords() {
        return totalWords;
    }

    public Integer getFillerWordCount() {
        return fillerWordCount;
    }

    public Integer getPauseCount() {
        return pauseCount;
    }

    public Double getClarityScore() {
        return clarityScore;
    }

    public String getMetricsJson() {
        return metricsJson;
    }

    public String getFeedbackJson() {
        return feedbackJson;
    }

    public String getOverallSummary() {
        return overallSummary;
    }

    public String getStrengthsJson() {
        return strengthsJson;
    }

    public String getImprovementsJson() {
        return improvementsJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public boolean isCompleted() {
        return status == EvaluationStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == EvaluationStatus.FAILED;
    }
}
