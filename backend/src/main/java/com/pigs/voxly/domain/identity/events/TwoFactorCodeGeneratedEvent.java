package com.pigs.voxly.domain.identity.events;

import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.sharedKernel.domain.events.DomainEvent;

public final class TwoFactorCodeGeneratedEvent extends DomainEvent {

    private final UserId userId;
    private final String email;
    private final String username;
    private final String code;

    public TwoFactorCodeGeneratedEvent(UserId userId, String email, String username, String code) {
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.code = code;
    }

    public UserId getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getCode() {
        return code;
    }
}
