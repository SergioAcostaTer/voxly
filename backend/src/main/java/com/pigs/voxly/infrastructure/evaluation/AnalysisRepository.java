package com.pigs.voxly.infrastructure.evaluation;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pigs.voxly.domain.evaluation.Analysis;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, UUID> {
    Optional<Analysis> findBySessionId(UUID sessionId);

    Optional<Analysis> findByUserIdAndSessionId(UUID userId, UUID sessionId);
}
