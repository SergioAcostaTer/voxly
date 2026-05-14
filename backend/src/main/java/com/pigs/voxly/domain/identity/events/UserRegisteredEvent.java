package com.pigs.voxly.domain.identity.events;

import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.sharedKernel.domain.events.DomainEvent;

public final class UserRegisteredEvent extends DomainEvent {

    private final UserId userId;
    private final String email;
    private final String username;
    private final String emailVerificationToken;

    public UserRegisteredEvent(UserId userId, String email, String username, String emailVerificationToken) {
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.emailVerificationToken = emailVerificationToken;
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

    public String getEmailVerificationToken() {
        return emailVerificationToken;
    }
}
