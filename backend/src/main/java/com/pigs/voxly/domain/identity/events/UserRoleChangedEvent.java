package com.pigs.voxly.domain.identity.events;

import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.domain.identity.enumerations.Role;
import com.pigs.voxly.sharedKernel.domain.events.DomainEvent;

public final class UserRoleChangedEvent extends DomainEvent {

    public enum Action { ADDED, REMOVED }

    private final UserId userId;
    private final Role role;
    private final Action action;

    public UserRoleChangedEvent(UserId userId, Role role, Action action) {
        this.userId = userId;
        this.role = role;
        this.action = action;
    }

    public UserId getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    public Action getAction() {
        return action;
    }
}
