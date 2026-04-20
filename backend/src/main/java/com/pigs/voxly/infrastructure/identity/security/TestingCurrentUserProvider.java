package com.pigs.voxly.infrastructure.identity.security;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.pigs.voxly.application.identity.ports.CurrentUserProvider;
import com.pigs.voxly.domain.identity.User;
import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.domain.identity.UserRepository;
import com.pigs.voxly.domain.identity.entities.TwoFactorCode;
import com.pigs.voxly.domain.identity.enumerations.Role;
import com.pigs.voxly.domain.identity.valueobjects.Email;
import com.pigs.voxly.domain.identity.valueobjects.PasswordHash;
import com.pigs.voxly.domain.identity.valueobjects.RefreshToken;
import com.pigs.voxly.domain.identity.valueobjects.Username;

@Component
@Primary
@ConditionalOnProperty(name = "app.auth.testing-open-access", havingValue = "true")
public class TestingCurrentUserProvider implements CurrentUserProvider {

    private final UserRepository userRepository;
    private final String email;
    private final String username;
    private final UUID userId;

    public TestingCurrentUserProvider(
            UserRepository userRepository,
            @Value("${app.auth.dev-email:test@voxly.local}") String email,
            @Value("${app.auth.dev-username:testuser}") String username,
            @Value("${app.auth.dev-user-id:11111111-1111-1111-1111-111111111111}") UUID userId) {
        this.userRepository = userRepository;
        this.email = email;
        this.username = username;
        this.userId = userId;
    }

    @Override
    public Optional<UUID> getUserId() {
        return Optional.of(ensureTestingUser().getId().getValue());
    }

    @Override
    public Optional<String> getEmail() {
        return Optional.of(ensureTestingUser().getEmail().getValue());
    }

    @Override
    public Optional<String> getUsername() {
        return Optional.of(ensureTestingUser().getUsername().getValue());
    }

    @Override
    public List<String> getRoles() {
        return ensureTestingUser().getRoles().stream()
                .map(Role::getName)
                .toList();
    }

    @Override
    public boolean isAuthenticated() {
        ensureTestingUser();
        return true;
    }

    @Override
    public boolean isInRole(String role) {
        return getRoles().contains(role);
    }

    private User ensureTestingUser() {
        return userRepository.findById(UserId.from(userId))
                .orElseGet(this::createTestingUser);
    }

    private User createTestingUser() {
        Instant now = Instant.now();
        User user = User.reconstitute(
                UserId.from(userId),
                Email.reconstitute(email),
                Username.reconstitute(username),
                PasswordHash.reconstitute("testing-bypass"),
                true,
                now,
                now,
                true,
                null,
                null,
                null,
                null,
                0,
                null,
                false,
                List.of(Role.USER),
                List.<RefreshToken>of(),
                List.<TwoFactorCode>of());

        userRepository.save(user);
        return user;
    }
}
