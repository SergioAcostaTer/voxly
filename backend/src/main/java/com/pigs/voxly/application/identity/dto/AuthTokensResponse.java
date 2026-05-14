package com.pigs.voxly.application.identity.dto;

import java.time.Instant;

public record AuthTokensResponse(
        String accessToken,
        Instant accessTokenExpiry,
        String refreshToken,
        Instant refreshTokenExpiry
) {
}
