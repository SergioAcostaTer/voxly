package com.pigs.voxly.domain.identity.events;

import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.sharedKernel.domain.events.DomainEvent;

import java.time.Instant;

public final class UserLockedOutEvent extends DomainEvent {

    private final UserId userId;
    private final Instant lockoutEnd;
    private final int failedAttempts;

    public UserLockedOutEvent(UserId userId, Instant lockoutEnd, int failedAttempts) {
        this.userId = userId;
        this.lockoutEnd = lockoutEnd;
        this.failedAttempts = failedAttempts;
    }

    public UserId getUserId() {
        return userId;
    }

    public Instant getLockoutEnd() {
        return lockoutEnd;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }
}
