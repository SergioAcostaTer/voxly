package com.pigs.voxly.infrastructure.evaluation.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.pigs.voxly.domain.evaluation.Evaluation;
import com.pigs.voxly.domain.evaluation.EvaluationId;
import com.pigs.voxly.domain.evaluation.EvaluationRepository;
import com.pigs.voxly.domain.evaluation.enumerations.EvaluationStatus;
import com.pigs.voxly.domain.sessions.SessionId;
import com.pigs.voxly.infrastructure.evaluation.persistence.mapper.EvaluationMapper;

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
    public List<Evaluation> findByStatuses(List<EvaluationStatus> statuses) {
        List<String> names = statuses.stream().map(EvaluationStatus::getName).toList();
        return springDataRepository.findByStatusIn(names).stream()
                .map(mapper::toDomain)
                .toList();
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
