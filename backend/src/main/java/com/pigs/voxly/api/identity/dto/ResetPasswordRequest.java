package com.pigs.voxly.api.identity.dto;

public record ResetPasswordRequest(String token, String newPassword) {
}
