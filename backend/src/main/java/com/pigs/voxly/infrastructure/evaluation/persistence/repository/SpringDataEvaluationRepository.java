package com.pigs.voxly.infrastructure.evaluation.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pigs.voxly.infrastructure.evaluation.persistence.entity.EvaluationJpaEntity;

@Repository
public interface SpringDataEvaluationRepository extends JpaRepository<EvaluationJpaEntity, UUID> {

    Optional<EvaluationJpaEntity> findBySessionId(UUID sessionId);

    List<EvaluationJpaEntity> findByStatusIn(List<String> statuses);
}
