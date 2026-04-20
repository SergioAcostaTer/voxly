package com.pigs.voxly.application.sessions.dto;

import java.time.Instant;
import java.util.UUID;

public record RecordingUploadResponse(
        UUID uploadId,
        long sizeBytes,
        int nextSequence,
        boolean completed,
        Instant modifiedAt
) {
}
