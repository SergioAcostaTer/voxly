package com.pigs.voxly.api.identity.dto;

import com.pigs.voxly.application.identity.dto.AuthTokensResponse;

import java.time.Instant;

public record AccessTokenResponse(
        String accessToken,
        Instant accessTokenExpiry
) {

    public static AccessTokenResponse from(AuthTokensResponse tokens) {
        return new AccessTokenResponse(tokens.accessToken(), tokens.accessTokenExpiry());
    }
}
