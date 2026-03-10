package com.pigs.voxly.api.identity.cookie;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.pigs.voxly.infrastructure.identity.config.JwtProperties;

@Component
public class AccessTokenCookieHelper {

    static final String COOKIE_NAME = "access_token";
    private static final String COOKIE_PATH = "/v1/auth";

    private final JwtProperties jwtProperties;

    public AccessTokenCookieHelper(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public ResponseCookie createCookie(String accessToken) {
        return ResponseCookie.from(COOKIE_NAME, accessToken)
                .httpOnly(true)
                .secure(jwtProperties.accessCookieSecure())
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(Duration.ofDays(jwtProperties.refreshTokenExpiryDays()))
                .build();
    }

    public ResponseCookie clearCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(jwtProperties.accessCookieSecure())
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
    }
}
