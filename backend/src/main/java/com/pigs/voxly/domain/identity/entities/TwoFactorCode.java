package com.pigs.voxly.domain.identity.entities;

import com.pigs.voxly.sharedKernel.domain.ddd.Entity;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class TwoFactorCode extends Entity<UUID> {

    private static final Duration CODE_VALIDITY = Duration.ofMinutes(10);

    private final String code;
    private final Instant createdAt;
    private final Instant expiresAt;
    private boolean used;
    private Instant usedAt;

    private TwoFactorCode(String code) {
        super(UUID.randomUUID());
        this.code = code;
        this.createdAt = Instant.now();
        this.expiresAt = Instant.now().plus(CODE_VALIDITY);
        this.used = false;
    }

    private TwoFactorCode(UUID id, String code, Instant createdAt, Instant expiresAt, boolean used, Instant usedAt) {
        super(id);
        this.code = code;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.used = used;
        this.usedAt = usedAt;
    }

    public static TwoFactorCode reconstitute(UUID id, String code, Instant createdAt, Instant expiresAt,
                                              boolean used, Instant usedAt) {
        return new TwoFactorCode(id, code, createdAt, expiresAt, used, usedAt);
    }

    public static TwoFactorCode generate() {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(100_000, 1_000_000));
        return new TwoFactorCode(code);
    }

    public boolean isValid() {
        return !used && Instant.now().isBefore(expiresAt);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean validate(String code) {
        return this.code.equals(code) && isValid();
    }

    public void markAsUsed() {
        this.used = true;
        this.usedAt = Instant.now();
    }

    public String getCode() {
        return code;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public Instant getUsedAt() {
        return usedAt;
    }
}
