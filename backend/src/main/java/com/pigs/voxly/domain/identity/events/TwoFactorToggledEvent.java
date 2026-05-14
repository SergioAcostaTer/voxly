package com.pigs.voxly.domain.identity.events;

import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.sharedKernel.domain.events.DomainEvent;

public final class TwoFactorToggledEvent extends DomainEvent {

    private final UserId userId;
    private final boolean enabled;

    public TwoFactorToggledEvent(UserId userId, boolean enabled) {
        this.userId = userId;
        this.enabled = enabled;
    }

    public UserId getUserId() {
        return userId;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
