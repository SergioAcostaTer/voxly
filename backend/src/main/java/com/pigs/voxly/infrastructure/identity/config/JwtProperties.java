package com.pigs.voxly.infrastructure.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secretKey,
        String issuer,
        String audience,
        int accessTokenExpiryMinutes,
        int refreshTokenExpiryDays,
        boolean accessCookieSecure) {

    public JwtProperties {
        if (issuer == null)
            issuer = "VoxLy";
        if (audience == null)
            audience = "VoxLy";
        if (accessTokenExpiryMinutes <= 0)
            accessTokenExpiryMinutes = 15;
        if (refreshTokenExpiryDays <= 0)
            refreshTokenExpiryDays = 7;
    }
}
