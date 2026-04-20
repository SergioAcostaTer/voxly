package com.pigs.voxly.infrastructure.evaluation.persistence.mapper;

import com.pigs.voxly.domain.evaluation.Evaluation;
import com.pigs.voxly.domain.evaluation.EvaluationId;
import com.pigs.voxly.domain.evaluation.enumerations.EvaluationStatus;
import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.domain.sessions.SessionId;
import com.pigs.voxly.infrastructure.evaluation.persistence.entity.EvaluationJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class EvaluationMapper {

    public Evaluation toDomain(EvaluationJpaEntity entity) {
        return Evaluation.reconstitute(
                EvaluationId.from(entity.getId()),
                SessionId.from(entity.getSessionId()),
                UserId.from(entity.getUserId()),
                EvaluationStatus.fromName(entity.getStatus()),
                entity.getSessionType(),
                entity.getTranscriptionText(),
                entity.getTranscriptionJson(),
                entity.getDurationSeconds(),
                entity.getDetectedLanguage(),
                entity.getWordsPerMinute(),
                entity.getTotalWords(),
                entity.getFillerWordCount(),
                entity.getPauseCount(),
                entity.getClarityScore(),
                entity.getMetricsJson(),
                entity.getFeedbackJson(),
                entity.getOverallSummary(),
                entity.getStrengthsJson(),
                entity.getImprovementsJson(),
                entity.getErrorMessage(),
                entity.getProcessingStartedAt(),
                entity.getCreatedAt(),
                entity.getCompletedAt()
        );
    }

    public EvaluationJpaEntity toEntity(Evaluation evaluation) {
        EvaluationJpaEntity entity = new EvaluationJpaEntity();
        entity.setId(evaluation.getId().getValue());
        entity.setSessionId(evaluation.getSessionId().getValue());
        entity.setUserId(evaluation.getUserId().getValue());
        entity.setStatus(evaluation.getStatus().getName());
        entity.setSessionType(evaluation.getSessionType());
        entity.setTranscriptionText(evaluation.getTranscriptionText());
        entity.setTranscriptionJson(evaluation.getTranscriptionJson());
        entity.setDurationSeconds(evaluation.getDurationSeconds());
        entity.setDetectedLanguage(evaluation.getDetectedLanguage());
        entity.setWordsPerMinute(evaluation.getWordsPerMinute());
        entity.setTotalWords(evaluation.getTotalWords());
        entity.setFillerWordCount(evaluation.getFillerWordCount());
        entity.setPauseCount(evaluation.getPauseCount());
        entity.setClarityScore(evaluation.getClarityScore());
        entity.setMetricsJson(evaluation.getMetricsJson());
        entity.setFeedbackJson(evaluation.getFeedbackJson());
        entity.setOverallSummary(evaluation.getOverallSummary());
        entity.setStrengthsJson(evaluation.getStrengthsJson());
        entity.setImprovementsJson(evaluation.getImprovementsJson());
        entity.setErrorMessage(evaluation.getErrorMessage());
        entity.setProcessingStartedAt(evaluation.getProcessingStartedAt());
        entity.setCreatedAt(evaluation.getCreatedAt());
        entity.setCompletedAt(evaluation.getCompletedAt());
        return entity;
    }
}
