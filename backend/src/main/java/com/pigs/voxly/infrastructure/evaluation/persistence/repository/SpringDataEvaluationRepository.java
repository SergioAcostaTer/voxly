package com.pigs.voxly.infrastructure.evaluation.persistence.repository;

import com.pigs.voxly.infrastructure.evaluation.persistence.entity.EvaluationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataEvaluationRepository extends JpaRepository<EvaluationJpaEntity, UUID> {

    Optional<EvaluationJpaEntity> findBySessionId(UUID sessionId);
}
