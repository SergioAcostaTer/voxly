package com.pigs.voxly.infrastructure.sessions.persistence.mapper;

import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.domain.sessions.Session;
import com.pigs.voxly.domain.sessions.SessionId;
import com.pigs.voxly.domain.sessions.enumerations.SessionStatus;
import com.pigs.voxly.domain.sessions.enumerations.SessionType;
import com.pigs.voxly.domain.sessions.valueobjects.MediaFile;
import com.pigs.voxly.domain.sessions.valueobjects.SessionTitle;
import com.pigs.voxly.infrastructure.sessions.persistence.entity.SessionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class SessionMapper {

    public Session toDomain(SessionJpaEntity entity) {
        MediaFile mediaFile = null;
        if (entity.getMediaStoragePath() != null) {
            mediaFile = MediaFile.reconstitute(
                    entity.getMediaStoragePath(),
                    entity.getMediaOriginalFilename(),
                    entity.getMediaContentType(),
                    entity.getMediaSizeBytes() != null ? entity.getMediaSizeBytes() : 0,
                    entity.getMediaDurationSeconds()
            );
        }

        return Session.reconstitute(
                SessionId.from(entity.getId()),
                UserId.from(entity.getUserId()),
                SessionTitle.reconstitute(entity.getTitle()),
                entity.getDescription(),
                SessionType.fromName(entity.getSessionType()),
                SessionStatus.fromName(entity.getStatus()),
                mediaFile,
                entity.getCreatedAt(),
                entity.getModifiedAt(),
                entity.getEvaluationId()
        );
    }

    public SessionJpaEntity toEntity(Session session) {
        SessionJpaEntity entity = new SessionJpaEntity();
        entity.setId(session.getId().getValue());
        entity.setUserId(session.getUserId().getValue());
        entity.setTitle(session.getTitle().getValue());
        entity.setDescription(session.getDescription());
        entity.setSessionType(session.getSessionType().getName());
        entity.setStatus(session.getStatus().getName());

        if (session.getMediaFile() != null) {
            entity.setMediaStoragePath(session.getMediaFile().getStoragePath());
            entity.setMediaOriginalFilename(session.getMediaFile().getOriginalFileName());
            entity.setMediaContentType(session.getMediaFile().getContentType());
            entity.setMediaSizeBytes(session.getMediaFile().getSizeBytes());
            entity.setMediaDurationSeconds(session.getMediaFile().getDurationSeconds());
        }

        entity.setEvaluationId(session.getEvaluationId());
        entity.setCreatedAt(session.getCreatedAt());
        entity.setModifiedAt(session.getModifiedAt());

        return entity;
    }
}
