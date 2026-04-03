package com.pigs.voxly.application.evaluation.dto;

import com.pigs.voxly.domain.evaluation.Evaluation;

import java.time.Instant;
import java.util.UUID;

public record EvaluationResponse(
        UUID id,
        UUID sessionId,
        String status,
        String sessionType,
        TranscriptionData transcription,
        MetricsData metrics,
        FeedbackData feedback,
        String errorMessage,
        Instant createdAt,
        Instant completedAt
) {
    public static EvaluationResponse fromDomain(Evaluation evaluation) {
        TranscriptionData transcription = null;
        if (evaluation.getTranscriptionText() != null) {
            transcription = new TranscriptionData(
                    evaluation.getTranscriptionText(),
                    evaluation.getDurationSeconds(),
                    evaluation.getDetectedLanguage()
            );
        }

        MetricsData metrics = null;
        if (evaluation.getWordsPerMinute() != null) {
            metrics = new MetricsData(
                    evaluation.getWordsPerMinute(),
                    evaluation.getTotalWords(),
                    evaluation.getFillerWordCount(),
                    evaluation.getPauseCount(),
                    evaluation.getClarityScore()
            );
        }

        FeedbackData feedback = null;
        if (evaluation.getOverallSummary() != null) {
            feedback = new FeedbackData(
                    evaluation.getOverallSummary(),
                    evaluation.getFeedbackJson()
            );
        }

        return new EvaluationResponse(
                evaluation.getId().getValue(),
                evaluation.getSessionId().getValue(),
                evaluation.getStatus().getName(),
                evaluation.getSessionType(),
                transcription,
                metrics,
                feedback,
                evaluation.getErrorMessage(),
                evaluation.getCreatedAt(),
                evaluation.getCompletedAt()
        );
    }

    public record TranscriptionData(
            String fullText,
            Double durationSeconds,
            String detectedLanguage
    ) {}

    public record MetricsData(
            Integer wordsPerMinute,
            Integer totalWords,
            Integer fillerWordCount,
            Integer pauseCount,
            Double clarityScore
    ) {}

    public record FeedbackData(
            String overallSummary,
            String notesJson
    ) {}
}
