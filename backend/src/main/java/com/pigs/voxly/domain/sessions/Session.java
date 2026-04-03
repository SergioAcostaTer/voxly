package com.pigs.voxly.domain.sessions;

import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.domain.sessions.enumerations.SessionStatus;
import com.pigs.voxly.domain.sessions.enumerations.SessionType;
import com.pigs.voxly.domain.sessions.events.*;
import com.pigs.voxly.domain.sessions.valueobjects.MediaFile;
import com.pigs.voxly.domain.sessions.valueobjects.SessionTitle;
import com.pigs.voxly.sharedKernel.domain.ddd.AggregateRoot;
import com.pigs.voxly.sharedKernel.domain.results.Result;
import com.pigs.voxly.sharedKernel.domain.results.ResultT;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Session extends AggregateRoot<SessionId> {

    private UserId userId;
    private SessionTitle title;
    private String description;
    private SessionType sessionType;
    private SessionStatus status;
    private MediaFile mediaFile;
    private Instant createdAt;
    private Instant modifiedAt;
    private UUID evaluationId;

    private Session() {}

    private Session(SessionId id, UserId userId, SessionTitle title, String description, SessionType sessionType) {
        super(id);
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.sessionType = sessionType;
        this.status = SessionStatus.DRAFT;
        this.createdAt = Instant.now();
        this.modifiedAt = Instant.now();
    }

    // ===== Factory =====

    public static ResultT<Session> create(UserId userId, SessionTitle title, String description, SessionType sessionType) {
        var session = new Session(SessionId.create(), userId, title, description, sessionType);

        session.raiseDomainEvent(new SessionCreatedEvent(
                session.getId(),
                userId,
                title.getValue(),
                sessionType.getName()
        ));

        return ResultT.success(session);
    }

    // ===== Reconstitution =====

    public static Session reconstitute(
            SessionId id,
            UserId userId,
            SessionTitle title,
            String description,
            SessionType sessionType,
            SessionStatus status,
            MediaFile mediaFile,
            Instant createdAt,
            Instant modifiedAt,
            UUID evaluationId
    ) {
        var session = new Session();
        session.id = id;
        session.userId = userId;
        session.title = title;
        session.description = description;
        session.sessionType = sessionType;
        session.status = status;
        session.mediaFile = mediaFile;
        session.createdAt = createdAt;
        session.modifiedAt = modifiedAt;
        session.evaluationId = evaluationId;
        return session;
    }

    // ===== Media Operations =====

    public Result uploadMedia(MediaFile media) {
        if (status == SessionStatus.ANALYZING) {
            return Result.failure(SessionErrors.CANNOT_MODIFY_ANALYZING);
        }

        this.mediaFile = media;
        this.status = SessionStatus.UPLOADED;
        markModified();

        raiseDomainEvent(new SessionMediaUploadedEvent(
                getId(),
                media.getStoragePath(),
                media.getContentType(),
                media.getSizeBytes()
        ));

        return Result.success();
    }

    public Result updateMediaDuration(double durationSeconds) {
        if (mediaFile == null) {
            return Result.failure(SessionErrors.NO_MEDIA_UPLOADED);
        }

        this.mediaFile = mediaFile.withDuration(durationSeconds);
        markModified();

        return Result.success();
    }

    // ===== Analysis Operations =====

    public Result requestAnalysis() {
        if (status == SessionStatus.DRAFT || mediaFile == null) {
            return Result.failure(SessionErrors.CANNOT_ANALYZE_DRAFT);
        }

        if (status == SessionStatus.ANALYZING) {
            return Result.failure(SessionErrors.ANALYSIS_ALREADY_REQUESTED);
        }

        this.status = SessionStatus.ANALYZING;
        markModified();

        raiseDomainEvent(new SessionAnalysisRequestedEvent(
                getId(),
                userId,
                mediaFile.getStoragePath(),
                sessionType.getName()
        ));

        return Result.success();
    }

    public Result completeAnalysis(UUID evaluationId) {
        if (status != SessionStatus.ANALYZING) {
            return Result.failure(SessionErrors.INVALID_STATUS_TRANSITION);
        }

        this.status = SessionStatus.COMPLETED;
        this.evaluationId = evaluationId;
        markModified();

        return Result.success();
    }

    public Result failAnalysis() {
        if (status != SessionStatus.ANALYZING) {
            return Result.failure(SessionErrors.INVALID_STATUS_TRANSITION);
        }

        this.status = SessionStatus.FAILED;
        markModified();

        return Result.success();
    }

    // ===== Update Operations =====

    public Result updateDetails(SessionTitle newTitle, String newDescription, SessionType newType) {
        if (status == SessionStatus.ANALYZING) {
            return Result.failure(SessionErrors.CANNOT_MODIFY_ANALYZING);
        }

        this.title = newTitle;
        this.description = newDescription;
        this.sessionType = newType;
        markModified();

        return Result.success();
    }

    // ===== Delete =====

    public ResultT<List<String>> prepareForDeletion() {
        if (status == SessionStatus.ANALYZING) {
            return ResultT.failure(SessionErrors.CANNOT_DELETE_ANALYZING);
        }

        List<String> pathsToDelete = new ArrayList<>();
        if (mediaFile != null) {
            pathsToDelete.add(mediaFile.getStoragePath());
        }

        raiseDomainEvent(new SessionDeletedEvent(getId(), pathsToDelete));

        return ResultT.success(pathsToDelete);
    }

    // ===== Ownership Check =====

    public boolean isOwnedBy(UserId userId) {
        return this.userId.equals(userId);
    }

    // ===== Getters =====

    public UserId getUserId() {
        return userId;
    }

    public SessionTitle getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public MediaFile getMediaFile() {
        return mediaFile;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getModifiedAt() {
        return modifiedAt;
    }

    public UUID getEvaluationId() {
        return evaluationId;
    }

    public boolean hasMedia() {
        return mediaFile != null;
    }

    public boolean isCompleted() {
        return status == SessionStatus.COMPLETED;
    }

    // ===== Private Helpers =====

    private void markModified() {
        this.modifiedAt = Instant.now();
    }
}
