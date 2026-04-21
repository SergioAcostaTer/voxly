package com.pigs.voxly.application.evaluation;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.pigs.voxly.domain.evaluation.EvaluationRepository;
import com.pigs.voxly.domain.evaluation.enumerations.EvaluationStatus;

@Service
public class EvaluationRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationRecoveryService.class);

    private final EvaluationRepository evaluationRepository;
    private final EvaluationProcessor evaluationProcessor;
    private final Duration staleThreshold;

    public EvaluationRecoveryService(EvaluationRepository evaluationRepository,
            EvaluationProcessor evaluationProcessor,
            @org.springframework.beans.factory.annotation.Value("${app.evaluation.stale-threshold-minutes:10}") long staleThresholdMinutes) {
        this.evaluationRepository = evaluationRepository;
        this.evaluationProcessor = evaluationProcessor;
        this.staleThreshold = Duration.ofMinutes(staleThresholdMinutes);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverOnStartup() {
        recoverPendingEvaluations("startup");
    }

    @Scheduled(fixedDelayString = "${app.evaluation.recovery-interval-ms:30000}")
    public void recoverOnSchedule() {
        recoverPendingEvaluations("scheduled");
    }

    private void recoverPendingEvaluations(String trigger) {
        var statuses = List.of(
                EvaluationStatus.PENDING,
                EvaluationStatus.TRANSCRIBING,
                EvaluationStatus.ANALYZING,
                EvaluationStatus.FAILED);
        var candidates = evaluationRepository.findByStatuses(statuses);
        var staleCutoff = Instant.now().minus(staleThreshold);

        if (candidates.isEmpty()) {
            return;
        }

        log.info("Evaluation recovery ({}) found {} unfinished evaluations", trigger, candidates.size());

        for (var evaluation : candidates) {
            if (evaluation.isCompleted()) {
                continue;
            }

            if (evaluation.isFailed()) {
                if (!evaluation.canRetry(staleCutoff)) {
                    continue;
                }
                var retryResult = evaluation.retry();
                if (retryResult.isFailure()) {
                    continue;
                }
                evaluationRepository.save(evaluation);
                log.info("Retrying failed evaluation {}", evaluation.getId().getValue());
                evaluationProcessor.processAsync(evaluation.getId().getValue());
                continue;
            }

            if (!evaluation.isProcessingStale(staleCutoff)) {
                continue;
            }
            evaluationProcessor.processAsync(evaluation.getId().getValue());
        }
    }
}
