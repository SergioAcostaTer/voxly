package com.pigs.voxly.domain.sessions.events;

import com.pigs.voxly.domain.sessions.SessionId;
import com.pigs.voxly.sharedKernel.domain.events.DomainEvent;

import java.util.List;

public final class SessionDeletedEvent extends DomainEvent {

    private final SessionId sessionId;
    private final List<String> storagePaths;

    public SessionDeletedEvent(SessionId sessionId, List<String> storagePaths) {
        super();
        this.sessionId = sessionId;
        this.storagePaths = List.copyOf(storagePaths);
    }

    public SessionId getSessionId() {
        return sessionId;
    }

    public List<String> getStoragePaths() {
        return storagePaths;
    }
}
