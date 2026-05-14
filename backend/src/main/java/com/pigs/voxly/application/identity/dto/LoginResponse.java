package com.pigs.voxly.application.identity.dto;

public record LoginResponse(
        UserResponse user,
        AuthTokensResponse tokens,
        boolean requiresTwoFactor
) {

    public static LoginResponse success(UserResponse user, AuthTokensResponse tokens) {
        return new LoginResponse(user, tokens, false);
    }

    public static LoginResponse twoFactorRequired(UserResponse user) {
        return new LoginResponse(user, null, true);
    }
}
