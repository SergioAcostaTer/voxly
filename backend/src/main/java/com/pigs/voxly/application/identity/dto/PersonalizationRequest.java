package com.pigs.voxly.application.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PersonalizationRequest(
        @NotBlank(message = "Professional role is required")
        @Size(max = 100, message = "Professional role must be at most 100 characters")
        String professionalRole,

        @NotBlank(message = "Primary goal is required")
        @Size(max = 100, message = "Primary goal must be at most 100 characters")
        String primaryGoal,

        @NotBlank(message = "Experience level is required")
        String experienceLevel
) {}
