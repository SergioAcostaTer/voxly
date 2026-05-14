package com.pigs.voxly.api.identity.dto;

import com.pigs.voxly.application.identity.dto.LoginResponse;
import com.pigs.voxly.application.identity.dto.UserResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginApiResponse(
        UserResponse user,
        AccessTokenResponse tokens,
        boolean requiresTwoFactor
) {

    public static LoginApiResponse from(LoginResponse loginResponse) {
        AccessTokenResponse tokens = loginResponse.tokens() != null
                ? AccessTokenResponse.from(loginResponse.tokens())
                : null;
        return new LoginApiResponse(loginResponse.user(), tokens, loginResponse.requiresTwoFactor());
    }
}
