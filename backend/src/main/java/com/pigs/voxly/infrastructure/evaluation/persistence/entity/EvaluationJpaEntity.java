package com.pigs.voxly.infrastructure.evaluation.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evaluations")
public class EvaluationJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false, unique = true)
    private UUID sessionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "session_type", length = 50)
    private String sessionType;

    @Column(name = "transcription_text", columnDefinition = "TEXT")
    private String transcriptionText;

    @Column(name = "transcription_json", columnDefinition = "TEXT")
    private String transcriptionJson;

    @Column(name = "duration_seconds")
    private Double durationSeconds;

    @Column(name = "detected_language", length = 10)
    private String detectedLanguage;

    @Column(name = "words_per_minute")
    private Integer wordsPerMinute;

    @Column(name = "total_words")
    private Integer totalWords;

    @Column(name = "filler_word_count")
    private Integer fillerWordCount;

    @Column(name = "pause_count")
    private Integer pauseCount;

    @Column(name = "clarity_score")
    private Double clarityScore;

    @Column(name = "metrics_json", columnDefinition = "TEXT")
    private String metricsJson;

    @Column(name = "feedback_json", columnDefinition = "TEXT")
    private String feedbackJson;

    @Column(name = "overall_summary", columnDefinition = "TEXT")
    private String overallSummary;

    @Column(name = "strengths_json", columnDefinition = "TEXT")
    private String strengthsJson;

    @Column(name = "improvements_json", columnDefinition = "TEXT")
    private String improvementsJson;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public EvaluationJpaEntity() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }

    public String getTranscriptionText() { return transcriptionText; }
    public void setTranscriptionText(String transcriptionText) { this.transcriptionText = transcriptionText; }

    public String getTranscriptionJson() { return transcriptionJson; }
    public void setTranscriptionJson(String transcriptionJson) { this.transcriptionJson = transcriptionJson; }

    public Double getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Double durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getDetectedLanguage() { return detectedLanguage; }
    public void setDetectedLanguage(String detectedLanguage) { this.detectedLanguage = detectedLanguage; }

    public Integer getWordsPerMinute() { return wordsPerMinute; }
    public void setWordsPerMinute(Integer wordsPerMinute) { this.wordsPerMinute = wordsPerMinute; }

    public Integer getTotalWords() { return totalWords; }
    public void setTotalWords(Integer totalWords) { this.totalWords = totalWords; }

    public Integer getFillerWordCount() { return fillerWordCount; }
    public void setFillerWordCount(Integer fillerWordCount) { this.fillerWordCount = fillerWordCount; }

    public Integer getPauseCount() { return pauseCount; }
    public void setPauseCount(Integer pauseCount) { this.pauseCount = pauseCount; }

    public Double getClarityScore() { return clarityScore; }
    public void setClarityScore(Double clarityScore) { this.clarityScore = clarityScore; }

    public String getMetricsJson() { return metricsJson; }
    public void setMetricsJson(String metricsJson) { this.metricsJson = metricsJson; }

    public String getFeedbackJson() { return feedbackJson; }
    public void setFeedbackJson(String feedbackJson) { this.feedbackJson = feedbackJson; }

    public String getOverallSummary() { return overallSummary; }
    public void setOverallSummary(String overallSummary) { this.overallSummary = overallSummary; }

    public String getStrengthsJson() { return strengthsJson; }
    public void setStrengthsJson(String strengthsJson) { this.strengthsJson = strengthsJson; }

    public String getImprovementsJson() { return improvementsJson; }
    public void setImprovementsJson(String improvementsJson) { this.improvementsJson = improvementsJson; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getProcessingStartedAt() { return processingStartedAt; }
    public void setProcessingStartedAt(Instant processingStartedAt) { this.processingStartedAt = processingStartedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
