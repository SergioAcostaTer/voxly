package com.pigs.voxly.application.evaluation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pigs.voxly.application.shared.ports.SpeechAnalysisService;
import com.pigs.voxly.application.shared.ports.StorageService;
import com.pigs.voxly.application.shared.ports.TranscriptionService;
import com.pigs.voxly.domain.evaluation.EvaluationId;
import com.pigs.voxly.domain.evaluation.EvaluationRepository;
import com.pigs.voxly.domain.sessions.Session;
import com.pigs.voxly.domain.sessions.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EvaluationProcessor {

    private static final Logger log = LoggerFactory.getLogger(EvaluationProcessor.class);

    private final EvaluationRepository evaluationRepository;
    private final SessionRepository sessionRepository;
    private final TranscriptionService transcriptionService;
    private final SpeechAnalysisService speechAnalysisService;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;

    public EvaluationProcessor(
            EvaluationRepository evaluationRepository,
            SessionRepository sessionRepository,
            TranscriptionService transcriptionService,
            SpeechAnalysisService speechAnalysisService,
            StorageService storageService,
            ObjectMapper objectMapper
    ) {
        this.evaluationRepository = evaluationRepository;
        this.sessionRepository = sessionRepository;
        this.transcriptionService = transcriptionService;
        this.speechAnalysisService = speechAnalysisService;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
    }

    @Async("analysisExecutor")
    @Transactional
    public void processAsync(UUID evaluationId) {
        log.info("Starting async evaluation processing for: {}", evaluationId);

        try {
            process(evaluationId);
        } catch (Exception e) {
            log.error("Failed to process evaluation: {}", evaluationId, e);
            failEvaluation(evaluationId, e.getMessage());
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

        // Step 1: Transcription
        log.info("Starting transcription for evaluation: {}", evaluationId);
        evaluation.startTranscription();
        evaluationRepository.save(evaluation);

        var mediaPath = storageService.getAbsolutePath(session.getMediaFile().getStoragePath());
        var transcriptionResult = transcriptionService.transcribe(mediaPath, null);

        if (transcriptionResult.isFailure()) {
            failEvaluation(evaluationId, "Transcription failed: " + transcriptionResult.getError().getMessage());
            return;
        }

        var transcription = transcriptionResult.getValue();

        try {
            String segmentsJson = objectMapper.writeValueAsString(transcription.segments());
            evaluation.completeTranscription(
                    transcription.fullText(),
                    segmentsJson,
                    transcription.durationSeconds(),
                    transcription.detectedLanguage()
            );
            evaluationRepository.save(evaluation);
        } catch (JsonProcessingException e) {
            failEvaluation(evaluationId, "Failed to serialize transcription");
            return;
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
                    improvementsJson
            );
            evaluationRepository.save(evaluation);

            // Update session with evaluation ID and status
            session.completeAnalysis(evaluation.getId().getValue());
            sessionRepository.save(session);

            log.info("Evaluation completed successfully: {}", evaluationId);

        } catch (JsonProcessingException e) {
            failEvaluation(evaluationId, "Failed to serialize analysis results");
        }
    }

    private void failEvaluation(UUID evaluationId, String errorMessage) {
        var evaluationOpt = evaluationRepository.findById(EvaluationId.from(evaluationId));
        if (evaluationOpt.isPresent()) {
            var evaluation = evaluationOpt.get();
            evaluation.fail(errorMessage);
            evaluationRepository.save(evaluation);

            // Also update session status
            var sessionOpt = sessionRepository.findById(evaluation.getSessionId());
            if (sessionOpt.isPresent()) {
                var session = sessionOpt.get();
                session.failAnalysis();
                sessionRepository.save(session);
            }
        }
        log.error("Evaluation failed: {} - {}", evaluationId, errorMessage);
    }
}
