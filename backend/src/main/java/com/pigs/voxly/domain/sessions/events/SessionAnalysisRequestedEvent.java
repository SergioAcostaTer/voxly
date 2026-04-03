package com.pigs.voxly.domain.sessions.events;

import com.pigs.voxly.domain.sessions.SessionId;
import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.sharedKernel.domain.events.DomainEvent;

public final class SessionAnalysisRequestedEvent extends DomainEvent {

    private final SessionId sessionId;
    private final UserId userId;
    private final String mediaStoragePath;
    private final String sessionType;

    public SessionAnalysisRequestedEvent(SessionId sessionId, UserId userId, String mediaStoragePath, String sessionType) {
        super();
        this.sessionId = sessionId;
        this.userId = userId;
        this.mediaStoragePath = mediaStoragePath;
        this.sessionType = sessionType;
    }

    public SessionId getSessionId() {
        return sessionId;
    }

    public UserId getUserId() {
        return userId;
    }

    public String getMediaStoragePath() {
        return mediaStoragePath;
    }

    public String getSessionType() {
        return sessionType;
    }
}
