package com.pigs.voxly.api.identity.dto;

public record RequestTwoFactorCodeRequest(String identifier, String password) {
}
