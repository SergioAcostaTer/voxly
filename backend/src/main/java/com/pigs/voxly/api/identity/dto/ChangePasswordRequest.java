package com.pigs.voxly.api.identity.dto;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
) {}
