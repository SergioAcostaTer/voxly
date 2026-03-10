package com.pigs.voxly.application.identity.dto;

import com.pigs.voxly.domain.identity.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String username,
        boolean active,
        boolean emailVerified,
        boolean twoFactorEnabled,
        List<String> roles,
        Instant createdAt,
        Instant modifiedAt
) {

    public static UserResponse fromDomain(User user) {
        return new UserResponse(
                user.getId().getValue(),
                user.getEmail().getValue(),
                user.getUsername().getValue(),
                user.isActive(),
                user.isEmailVerified(),
                user.isTwoFactorEnabled(),
                user.getRoles().stream().map(r -> r.getName()).toList(),
                user.getCreatedAt(),
                user.getModifiedAt());
    }
}
