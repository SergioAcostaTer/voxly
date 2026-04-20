package com.pigs.voxly.infrastructure.identity.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pigs.voxly.infrastructure.identity.persistence.entity.ConfigJpaEntity;

public interface SpringDataConfigRepository extends JpaRepository<ConfigJpaEntity, String> {
}
