package com.pigs.voxly.domain.identity.events;

import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.sharedKernel.domain.events.DomainEvent;

public final class UserEmailVerifiedEvent extends DomainEvent {

    private final UserId userId;

    public UserEmailVerifiedEvent(UserId userId) {
        this.userId = userId;
    }

    public UserId getUserId() {
        return userId;
    }
}
