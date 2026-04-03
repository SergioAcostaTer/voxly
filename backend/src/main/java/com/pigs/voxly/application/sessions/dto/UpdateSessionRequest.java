package com.pigs.voxly.application.sessions.dto;

import jakarta.validation.constraints.Size;

public record UpdateSessionRequest(
        @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
        String title,

        @Size(max = 2000, message = "Description cannot exceed 2000 characters")
        String description,

        String sessionType
) {}
