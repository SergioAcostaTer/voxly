package com.pigs.voxly.infrastructure.sessions.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sessions")
public class SessionJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "session_type", nullable = false, length = 50)
    private String sessionType;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "media_storage_path")
    private String mediaStoragePath;

    @Column(name = "media_original_filename")
    private String mediaOriginalFilename;

    @Column(name = "media_content_type", length = 100)
    private String mediaContentType;

    @Column(name = "media_size_bytes")
    private Long mediaSizeBytes;

    @Column(name = "media_duration_seconds")
    private Double mediaDurationSeconds;

    @Column(name = "evaluation_id")
    private UUID evaluationId;

    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "modified_at")
    private Instant modifiedAt;

    public SessionJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSessionType() {
        return sessionType;
    }

    public void setSessionType(String sessionType) {
        this.sessionType = sessionType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMediaStoragePath() {
        return mediaStoragePath;
    }

    public void setMediaStoragePath(String mediaStoragePath) {
        this.mediaStoragePath = mediaStoragePath;
    }

    public String getMediaOriginalFilename() {
        return mediaOriginalFilename;
    }

    public void setMediaOriginalFilename(String mediaOriginalFilename) {
        this.mediaOriginalFilename = mediaOriginalFilename;
    }

    public String getMediaContentType() {
        return mediaContentType;
    }

    public void setMediaContentType(String mediaContentType) {
        this.mediaContentType = mediaContentType;
    }

    public Long getMediaSizeBytes() {
        return mediaSizeBytes;
    }

    public void setMediaSizeBytes(Long mediaSizeBytes) {
        this.mediaSizeBytes = mediaSizeBytes;
    }

    public Double getMediaDurationSeconds() {
        return mediaDurationSeconds;
    }

    public void setMediaDurationSeconds(Double mediaDurationSeconds) {
        this.mediaDurationSeconds = mediaDurationSeconds;
    }

    public UUID getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(UUID evaluationId) {
        this.evaluationId = evaluationId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Instant modifiedAt) {
        this.modifiedAt = modifiedAt;
    }
}
