package com.pigs.voxly.application.identity.dto;

public record RegisterRequest(
        String email,
        String username,
        String password
) {
}
