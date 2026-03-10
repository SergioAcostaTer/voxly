package com.pigs.voxly.infrastructure.identity.security;

import com.pigs.voxly.application.identity.ports.JwtTokenProvider;
import com.pigs.voxly.domain.identity.User;
import com.pigs.voxly.infrastructure.identity.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProviderImpl implements JwtTokenProvider {

    private final JwtProperties properties;
    private final SecretKey signingKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtTokenProviderImpl(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secretKey().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.accessTokenExpiryMinutes(), ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(user.getId().getValue().toString())
                .claim("email", user.getEmail().getValue())
                .claim("username", user.getUsername().getValue())
                .claim("roles", user.getRoles().stream().map(r -> r.getName()).toList())
                .issuer(properties.issuer())
                .audience().add(properties.audience()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    @Override
    public String generateRefreshToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public Instant getAccessTokenExpiry() {
        return Instant.now().plus(properties.accessTokenExpiryMinutes(), ChronoUnit.MINUTES);
    }

    @Override
    public Instant getRefreshTokenExpiry() {
        return Instant.now().plus(properties.refreshTokenExpiryDays(), ChronoUnit.DAYS);
    }
}
