package com.pigs.voxly.infrastructure.sessions.persistence.repository;

import com.pigs.voxly.infrastructure.sessions.persistence.entity.SessionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataSessionRepository extends JpaRepository<SessionJpaEntity, UUID> {

    Optional<SessionJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    Page<SessionJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserId(UUID userId);
}
