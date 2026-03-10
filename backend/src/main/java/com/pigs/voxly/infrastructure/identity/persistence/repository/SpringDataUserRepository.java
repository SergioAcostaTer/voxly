package com.pigs.voxly.infrastructure.identity.persistence.repository;

import com.pigs.voxly.infrastructure.identity.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByEmail(String email);

    Optional<UserJpaEntity> findByUsername(String username);

    @Query("SELECT u FROM UserJpaEntity u WHERE u.email = :identifier OR u.username = :identifier")
    Optional<UserJpaEntity> findByEmailOrUsername(@Param("identifier") String identifier);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM UserJpaEntity u JOIN u.refreshTokens rt WHERE rt.token = :token")
    Optional<UserJpaEntity> findByRefreshToken(@Param("token") String token);

    Optional<UserJpaEntity> findByEmailVerificationToken(String token);

    Optional<UserJpaEntity> findByPasswordResetToken(String token);
}
