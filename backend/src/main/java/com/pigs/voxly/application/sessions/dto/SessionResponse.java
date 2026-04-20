package com.pigs.voxly.application.sessions.dto;

import java.time.Instant;
import java.util.UUID;

import com.pigs.voxly.domain.sessions.Session;

public record SessionResponse(
        UUID id,
        UUID userId,
        String title,
        String description,
        String sessionType,
        String status,
        MediaFileResponse mediaFile,
        UUID evaluationId,
        String language,
        Instant createdAt,
        Instant modifiedAt) {
    public static SessionResponse fromDomain(Session session) {
        return new SessionResponse(
                session.getId().getValue(),
                session.getUserId().getValue(),
                session.getTitle().getValue(),
                session.getDescription(),
                session.getSessionType().getName(),
                session.getStatus().getName(),
                session.getMediaFile() != null ? MediaFileResponse.fromDomain(session.getMediaFile()) : null,
                session.getEvaluationId(),
                session.getLanguage(),
                session.getCreatedAt(),
                session.getModifiedAt());
    }

    public record MediaFileResponse(
            String storagePath,
            String originalFileName,
            String contentType,
            long sizeBytes,
            Double durationSeconds,
            String url) {
        public static MediaFileResponse fromDomain(com.pigs.voxly.domain.sessions.valueobjects.MediaFile mediaFile) {
            return new MediaFileResponse(
                    mediaFile.getStoragePath(),
                    mediaFile.getOriginalFileName(),
                    mediaFile.getContentType(),
                    mediaFile.getSizeBytes(),
                    mediaFile.getDurationSeconds(),
                    "/api/v1/files/" + mediaFile.getStoragePath());
        }
    }
}
