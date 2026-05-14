package com.pigs.voxly.domain.evaluation;

import java.util.List;
import java.util.Optional;

import com.pigs.voxly.domain.evaluation.enumerations.EvaluationStatus;
import com.pigs.voxly.domain.sessions.SessionId;

public interface EvaluationRepository {

    Optional<Evaluation> findById(EvaluationId id);

    Optional<Evaluation> findBySessionId(SessionId sessionId);

    List<Evaluation> findByStatuses(List<EvaluationStatus> statuses);

    void save(Evaluation evaluation);

    void delete(Evaluation evaluation);
}
