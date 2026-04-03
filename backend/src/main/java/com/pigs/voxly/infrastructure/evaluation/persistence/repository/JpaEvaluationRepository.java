package com.pigs.voxly.infrastructure.evaluation.persistence.repository;

import com.pigs.voxly.domain.evaluation.Evaluation;
import com.pigs.voxly.domain.evaluation.EvaluationId;
import com.pigs.voxly.domain.evaluation.EvaluationRepository;
import com.pigs.voxly.domain.sessions.SessionId;
import com.pigs.voxly.infrastructure.evaluation.persistence.mapper.EvaluationMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaEvaluationRepository implements EvaluationRepository {

    private final SpringDataEvaluationRepository springDataRepository;
    private final EvaluationMapper mapper;

    public JpaEvaluationRepository(SpringDataEvaluationRepository springDataRepository, EvaluationMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Evaluation> findById(EvaluationId id) {
        return springDataRepository.findById(id.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Evaluation> findBySessionId(SessionId sessionId) {
        return springDataRepository.findBySessionId(sessionId.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public void save(Evaluation evaluation) {
        springDataRepository.save(mapper.toEntity(evaluation));
    }

    @Override
    public void delete(Evaluation evaluation) {
        springDataRepository.deleteById(evaluation.getId().getValue());
    }
}
