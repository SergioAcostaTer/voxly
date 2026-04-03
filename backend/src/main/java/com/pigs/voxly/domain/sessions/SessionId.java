package com.pigs.voxly.domain.sessions;

import com.pigs.voxly.sharedKernel.domain.types.StronglyTypedId;

import java.util.UUID;

public final class SessionId extends StronglyTypedId.UuidId<SessionId> {

    private SessionId(UUID value) {
        super(value);
    }

    public static SessionId create() {
        return new SessionId(UUID.randomUUID());
    }

    public static SessionId from(UUID value) {
        return new SessionId(value);
    }

    public static SessionId from(String value) {
        return new SessionId(UUID.fromString(value));
    }
}
