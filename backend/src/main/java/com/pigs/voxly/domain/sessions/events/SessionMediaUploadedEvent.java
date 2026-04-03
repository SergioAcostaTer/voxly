package com.pigs.voxly.domain.sessions.events;

import com.pigs.voxly.domain.sessions.SessionId;
import com.pigs.voxly.sharedKernel.domain.events.DomainEvent;

public final class SessionMediaUploadedEvent extends DomainEvent {

    private final SessionId sessionId;
    private final String storagePath;
    private final String contentType;
    private final long sizeBytes;

    public SessionMediaUploadedEvent(SessionId sessionId, String storagePath, String contentType, long sizeBytes) {
        super();
        this.sessionId = sessionId;
        this.storagePath = storagePath;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public SessionId getSessionId() {
        return sessionId;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }
}
