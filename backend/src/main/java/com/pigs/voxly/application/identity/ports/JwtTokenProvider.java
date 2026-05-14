package com.pigs.voxly.application.identity.ports;

import com.pigs.voxly.domain.identity.User;

import java.time.Instant;

public interface JwtTokenProvider {

    String generateAccessToken(User user);

    String generateRefreshToken();

    Instant getAccessTokenExpiry();

    Instant getRefreshTokenExpiry();
}
