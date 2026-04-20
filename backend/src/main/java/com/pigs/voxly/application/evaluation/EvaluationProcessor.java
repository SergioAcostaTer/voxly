package com.pigs.voxly.application.evaluation;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pigs.voxly.application.sessions.SessionStatusStreamService;
import com.pigs.voxly.application.shared.ports.SpeechAnalysisService;
import com.pigs.voxly.application.shared.ports.StorageService;
import com.pigs.voxly.application.shared.ports.TranscriptionService;
import com.pigs.voxly.domain.evaluation.EvaluationId;
import com.pigs.voxly.domain.evaluation.EvaluationRepository;
import com.pigs.voxly.domain.sessions.SessionRepository;
import com.pigs.voxly.application.shared.ports.TranscriptionService.Segment;
import com.pigs.voxly.application.shared.ports.TranscriptionService.Word;

@Service
public class EvaluationProcessor {

    private static final Logger log = LoggerFactory.getLogger(EvaluationProcessor.class);

    private final EvaluationRepository evaluationRepository;
    private final SessionRepository sessionRepository;
    private final TranscriptionService transcriptionService;
    private final SpeechAnalysisService speechAnalysisService;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;
    private final SessionStatusStreamService sessionStatusStreamService;
    private final ConcurrentHashMap<UUID, Boolean> activeEvaluations = new ConcurrentHashMap<>();

    public EvaluationProcessor(
            EvaluationRepository evaluationRepository,
            SessionRepository sessionRepository,
            TranscriptionService transcriptionService,
            SpeechAnalysisService speechAnalysisService,
            StorageService storageService,
            ObjectMapper objectMapper,
            SessionStatusStreamService sessionStatusStreamService) {
        this.evaluationRepository = evaluationRepository;
        this.sessionRepository = sessionRepository;
        this.transcriptionService = transcriptionService;
        this.speechAnalysisService = speechAnalysisService;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
        this.sessionStatusStreamService = sessionStatusStreamService;
    }

    @Async("analysisExecutor")
    @Transactional
    public void processAsync(UUID evaluationId) {
        if (activeEvaluations.putIfAbsent(evaluationId, Boolean.TRUE) != null) {
            log.info("Skipping async evaluation processing for {} because it is already active", evaluationId);
            return;
        }

        log.info("Starting async evaluation processing for: {}", evaluationId);

        try {
            process(evaluationId);
        } catch (Exception e) {
            log.error("Failed to process evaluation: {}", evaluationId, e);
            failEvaluation(evaluationId, e.getMessage());
        } finally {
            activeEvaluations.remove(evaluationId);
        }
    }

    private void process(UUID evaluationId) {
        var evaluationOpt = evaluationRepository.findById(EvaluationId.from(evaluationId));
        if (evaluationOpt.isEmpty()) {
            log.error("Evaluation not found: {}", evaluationId);
            return;
        }

        var evaluation = evaluationOpt.get();
        var sessionOpt = sessionRepository.findById(evaluation.getSessionId());
        if (sessionOpt.isEmpty()) {
            failEvaluation(evaluationId, "Session not found");
            return;
        }

        var session = sessionOpt.get();
        if (!session.hasMedia()) {
            failEvaluation(evaluationId, "Session has no media file");
            return;
        }

        if (evaluation.isCompleted() || evaluation.isFailed()) {
            log.info("Skipping evaluation {} because status is {}", evaluationId, evaluation.getStatus().getName());
            return;
        }

        Path mediaPath = null;
        try {
            mediaPath = storageService.getAbsolutePath(session.getMediaFile().getStoragePath());

            com.pigs.voxly.application.shared.ports.TranscriptionService.TranscriptionResult transcription;

            // Step 1: Transcription (skip if already done and status is ANALYZING)
            if ("analyzing".equalsIgnoreCase(evaluation.getStatus().getName())
                    && evaluation.getTranscriptionText() != null) {
                transcription = new com.pigs.voxly.application.shared.ports.TranscriptionService.TranscriptionResult(
                        evaluation.getTranscriptionText(),
                        parseSegmentsJson(evaluation.getTranscriptionJson()),
                        parseWordsJson(evaluation.getTranscriptionWordsJson()),
                        evaluation.getDetectedLanguage() != null ? evaluation.getDetectedLanguage()
                                : session.getLanguage(),
                        evaluation.getDurationSeconds() != null ? evaluation.getDurationSeconds() : 0.0,
                        evaluation.getTranscriptionRawJson());
            } else {
                log.info("Starting transcription for evaluation: {}", evaluationId);
                var startResult = evaluation.startTranscription();
                if (startResult.isFailure()) {
                    failEvaluation(evaluationId, "Cannot start transcription from current status");
                    return;
                }

                evaluationRepository.save(evaluation);
                sessionStatusStreamService.publishEvaluationUpdate(evaluation);

                var transcriptionResult = transcriptionService.transcribe(mediaPath, session.getLanguage());

                if (transcriptionResult.isFailure()) {
                    failEvaluation(evaluationId,
                            "Transcription failed: " + transcriptionResult.getError().getMessage());
                    return;
                }

                transcription = transcriptionResult.getValue();

                try {
                    String segmentsJson = objectMapper.writeValueAsString(transcription.segments());
                    String wordsJson = objectMapper.writeValueAsString(transcription.words());
                    evaluation.completeTranscription(
                            transcription.fullText(),
                            segmentsJson,
                            wordsJson,
                            transcription.rawJson(),
                            transcription.durationSeconds(),
                            transcription.detectedLanguage());
                    evaluationRepository.save(evaluation);
                    sessionStatusStreamService.publishEvaluationUpdate(evaluation);
                } catch (JsonProcessingException e) {
                    failEvaluation(evaluationId, "Failed to serialize transcription");
                    return;
                }
            }

            // Step 2: Analysis
            log.info("Starting analysis for evaluation: {}", evaluationId);
            var analysisResult = speechAnalysisService.analyze(transcription, session.getSessionType().getName());

            if (analysisResult.isFailure()) {
                failEvaluation(evaluationId, "Analysis failed: " + analysisResult.getError().getMessage());
                return;
            }

            var analysis = analysisResult.getValue();

            try {
                String metricsJson = objectMapper.writeValueAsString(analysis.metrics());
                String feedbackJson = objectMapper.writeValueAsString(analysis.feedbackNotes());
                String strengthsJson = objectMapper.writeValueAsString(analysis.strengths());
                String improvementsJson = objectMapper.writeValueAsString(analysis.areasForImprovement());

                evaluation.completeAnalysis(
                        analysis.metrics().wordsPerMinute(),
                        analysis.metrics().totalWords(),
                        analysis.metrics().fillerWordCount(),
                        analysis.metrics().pauseCount(),
                        analysis.metrics().clarityScore(),
                        metricsJson,
                        feedbackJson,
                        analysis.overallSummary(),
                        strengthsJson,
                        improvementsJson);
                evaluationRepository.save(evaluation);

                // Update session with evaluation ID and status
                session.completeAnalysis(evaluation.getId().getValue());
                sessionRepository.save(session);
                sessionStatusStreamService.publishEvaluationUpdate(evaluation);
                sessionStatusStreamService.publishSessionUpdate(session);

                log.info("Evaluation completed successfully: {}", evaluationId);

            } catch (JsonProcessingException e) {
                failEvaluation(evaluationId, "Failed to serialize analysis results");
            }
        } finally {
            storageService.cleanupTemporaryFile(mediaPath);
        }
    }

    private void failEvaluation(UUID evaluationId, String errorMessage) {
        var evaluationOpt = evaluationRepository.findById(EvaluationId.from(evaluationId));
        if (evaluationOpt.isPresent()) {
            var evaluation = evaluationOpt.get();
            evaluation.fail(errorMessage);
            evaluationRepository.save(evaluation);
            sessionStatusStreamService.publishEvaluationUpdate(evaluation);

            // Also update session status
            var sessionOpt = sessionRepository.findById(evaluation.getSessionId());
            if (sessionOpt.isPresent()) {
                var session = sessionOpt.get();
                session.failAnalysis();
                sessionRepository.save(session);
                sessionStatusStreamService.publishSessionUpdate(session);
            }
        }
        log.error("Evaluation failed: {} - {}", evaluationId, errorMessage);
    }

    private List<Segment> parseSegmentsJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, Segment.class));
        } catch (Exception e) {
            log.warn("Failed to parse stored transcript segments JSON", e);
            return List.of();
        }
    }

    private List<Word> parseWordsJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, Word.class));
        } catch (Exception e) {
            log.warn("Failed to parse stored transcript words JSON", e);
            return List.of();
        }
    }
}
