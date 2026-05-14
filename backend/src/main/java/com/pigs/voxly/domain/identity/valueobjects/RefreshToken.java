package com.pigs.voxly.domain.identity.valueobjects;

import com.pigs.voxly.domain.identity.UserErrors;
import com.pigs.voxly.sharedKernel.domain.ddd.ValueObject;
import com.pigs.voxly.sharedKernel.domain.results.ResultT;

import java.time.Instant;
import java.util.List;

public final class RefreshToken extends ValueObject {

    private final String token;
    private final Instant expiresAt;
    private final Instant createdAt;
    private final boolean revoked;
    private final Instant revokedAt;
    private final String replacedByToken;

    private RefreshToken(String token, Instant expiresAt, Instant createdAt,
                         boolean revoked, Instant revokedAt, String replacedByToken) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.revoked = revoked;
        this.revokedAt = revokedAt;
        this.replacedByToken = replacedByToken;
    }

    public static ResultT<RefreshToken> create(String token, Instant expiresAt) {
        if (token == null || token.isBlank()) {
            return ResultT.failure(UserErrors.REFRESH_TOKEN_REQUIRED);
        }
        if (expiresAt.isBefore(Instant.now())) {
            return ResultT.failure(UserErrors.REFRESH_TOKEN_EXPIRY_IN_PAST);
        }
        return ResultT.success(new RefreshToken(token, expiresAt, Instant.now(), false, null, null));
    }

    public static RefreshToken reconstitute(String token, Instant expiresAt, Instant createdAt,
                                            boolean revoked, Instant revokedAt, String replacedByToken) {
        return new RefreshToken(token, expiresAt, createdAt, revoked, revokedAt, replacedByToken);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return !revoked && !isExpired();
    }

    public RefreshToken revoke(String replacedByToken) {
        if (revoked) return this;
        return new RefreshToken(token, expiresAt, createdAt, true, Instant.now(), replacedByToken);
    }

    public RefreshToken revoke() {
        return revoke(null);
    }

    public String getToken() {
        return token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String getReplacedByToken() {
        return replacedByToken;
    }

    @Override
    protected List<Object> equalityComponents() {
        return List.of(token);
    }

    @Override
    public String toString() {
        return token.substring(0, Math.min(8, token.length())) + "...";
    }
}
