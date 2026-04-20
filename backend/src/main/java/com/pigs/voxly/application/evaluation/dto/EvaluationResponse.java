package com.pigs.voxly.application.evaluation.dto;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pigs.voxly.application.shared.ports.TranscriptionService;
import com.pigs.voxly.domain.evaluation.Evaluation;

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
        Instant completedAt) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static EvaluationResponse fromDomain(Evaluation evaluation) {
        TranscriptionData transcription = null;
        if (evaluation.getTranscriptionText() != null) {
            transcription = new TranscriptionData(
                    evaluation.getTranscriptionText(),
                    parseSegments(evaluation.getTranscriptionJson()),
                    parseWords(evaluation.getTranscriptionWordsJson()),
                    evaluation.getDurationSeconds(),
                    evaluation.getDetectedLanguage(),
                    evaluation.getTranscriptionRawJson());
        }

        MetricsData metrics = null;
        if (evaluation.getWordsPerMinute() != null) {
            metrics = new MetricsData(
                    evaluation.getWordsPerMinute(),
                    evaluation.getTotalWords(),
                    evaluation.getFillerWordCount(),
                    evaluation.getPauseCount(),
                    evaluation.getClarityScore());
        }

        FeedbackData feedback = null;
        if (evaluation.getOverallSummary() != null) {
            feedback = new FeedbackData(
                    evaluation.getOverallSummary(),
                    evaluation.getFeedbackJson(),
                    evaluation.getStrengthsJson(),
                    evaluation.getImprovementsJson());
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
                evaluation.getCompletedAt());
    }

    private static List<SegmentData> parseSegments(String transcriptionJson) {
        if (transcriptionJson == null || transcriptionJson.isBlank()) {
            return Collections.emptyList();
        }

        try {
            List<TranscriptionService.Segment> segments = OBJECT_MAPPER.readValue(
                    transcriptionJson,
                    new TypeReference<>() {
                    });

            return segments.stream()
                    .map(segment -> new SegmentData(
                            segment.text(),
                            segment.startSeconds(),
                            segment.endSeconds()))
                    .toList();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private static List<WordData> parseWords(String transcriptionWordsJson) {
        if (transcriptionWordsJson == null || transcriptionWordsJson.isBlank()) {
            return Collections.emptyList();
        }

        try {
            List<TranscriptionService.Word> words = OBJECT_MAPPER.readValue(
                    transcriptionWordsJson,
                    new TypeReference<>() {
                    });

            return words.stream()
                    .map(word -> new WordData(
                            word.word(),
                            word.startSeconds(),
                            word.endSeconds(),
                            word.confidence()))
                    .toList();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    public record TranscriptionData(
            String fullText,
            List<SegmentData> segments,
            List<WordData> words,
            Double durationSeconds,
            String detectedLanguage,
            String rawJson) {
    }

    public record SegmentData(
            String text,
            double startSeconds,
            double endSeconds) {
    }

    public record WordData(
            String word,
            double startSeconds,
            double endSeconds,
            double confidence) {
    }

    public record MetricsData(
            Integer wordsPerMinute,
            Integer totalWords,
            Integer fillerWordCount,
            Integer pauseCount,
            Double clarityScore) {
    }

    public record FeedbackData(
            String overallSummary,
            String notesJson,
            String strengthsJson,
            String areasForImprovementJson) {
    }
}
