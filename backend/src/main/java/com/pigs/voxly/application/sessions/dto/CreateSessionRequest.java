package com.pigs.voxly.application.sessions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSessionRequest(
        @NotBlank(message = "Title is required") @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters") String title,

        @Size(max = 2000, message = "Description cannot exceed 2000 characters") String description,

        @NotBlank(message = "Session type is required") String sessionType,

        @Size(min = 2, max = 10, message = "Language must be a valid language code (e.g., 'en', 'es')") String language) {
}
