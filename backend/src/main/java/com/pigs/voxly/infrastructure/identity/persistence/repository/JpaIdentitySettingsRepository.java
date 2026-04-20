package com.pigs.voxly.infrastructure.identity.persistence.repository;

import org.springframework.stereotype.Repository;

import com.pigs.voxly.application.identity.ports.IdentitySettingsRepository;

@Repository
public class JpaIdentitySettingsRepository implements IdentitySettingsRepository {

    private static final String REQUIRE_EMAIL_VERIFICATION_KEY = "auth.require_email_verification";

    private final SpringDataConfigRepository configRepository;

    public JpaIdentitySettingsRepository(SpringDataConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Override
    public boolean isEmailVerificationRequired() {
        return configRepository.findById(REQUIRE_EMAIL_VERIFICATION_KEY)
                .map(config -> config.isBooleanValue())
                .orElse(false);
    }
}
