package com.pigs.voxly.domain.sessions.events;

import com.pigs.voxly.domain.sessions.SessionId;
import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.sharedKernel.domain.events.DomainEvent;

public final class SessionCreatedEvent extends DomainEvent {

    private final SessionId sessionId;
    private final UserId userId;
    private final String title;
    private final String sessionType;

    public SessionCreatedEvent(SessionId sessionId, UserId userId, String title, String sessionType) {
        super();
        this.sessionId = sessionId;
        this.userId = userId;
        this.title = title;
        this.sessionType = sessionType;
    }

    public SessionId getSessionId() {
        return sessionId;
    }

    public UserId getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getSessionType() {
        return sessionType;
    }
}
