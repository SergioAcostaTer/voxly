package com.pigs.voxly.domain.evaluation;

import com.pigs.voxly.domain.sessions.SessionId;

import java.util.Optional;

public interface EvaluationRepository {

    Optional<Evaluation> findById(EvaluationId id);

    Optional<Evaluation> findBySessionId(SessionId sessionId);

    void save(Evaluation evaluation);

    void delete(Evaluation evaluation);
}
