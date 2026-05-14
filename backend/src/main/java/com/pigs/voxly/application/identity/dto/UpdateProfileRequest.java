package com.pigs.voxly.application.identity.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
        String username,
        @Size(max = 100, message = "Professional role must not exceed 100 characters")
        String professionalRole,
        @Size(max = 500, message = "Coaching focus must not exceed 500 characters")
        String coachingFocus
) {}
