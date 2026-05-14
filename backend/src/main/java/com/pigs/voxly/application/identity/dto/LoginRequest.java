package com.pigs.voxly.application.identity.dto;

public record LoginRequest(
        String identifier,
        String password,
        String twoFactorCode
) {

    public LoginRequest(String identifier, String password) {
        this(identifier, password, null);
    }

    public boolean hasTwoFactorCode() {
        return twoFactorCode != null && !twoFactorCode.isBlank();
    }
}
