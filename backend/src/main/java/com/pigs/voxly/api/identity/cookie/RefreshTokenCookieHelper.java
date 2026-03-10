package com.pigs.voxly.api.identity.cookie;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.pigs.voxly.infrastructure.identity.config.JwtProperties;

@Component
public class RefreshTokenCookieHelper {

    static final String COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/v1/auth";

    private final JwtProperties jwtProperties;

    public RefreshTokenCookieHelper(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public ResponseCookie createCookie(String refreshToken) {
        return ResponseCookie.from(COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(Duration.ofDays(jwtProperties.refreshTokenExpiryDays()))
                .build();
    }

    public ResponseCookie clearCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
    }
}
