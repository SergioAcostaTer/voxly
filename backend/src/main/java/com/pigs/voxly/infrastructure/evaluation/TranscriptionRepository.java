package com.pigs.voxly.infrastructure.evaluation;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pigs.voxly.domain.evaluation.Transcription;

@Repository
public interface TranscriptionRepository extends JpaRepository<Transcription, UUID> {
    Optional<Transcription> findBySessionId(UUID sessionId);

    Optional<Transcription> findByUserIdAndSessionId(UUID userId, UUID sessionId);
}
