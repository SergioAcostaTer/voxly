package com.pigs.voxly.domain.identity;

import com.pigs.voxly.domain.identity.entities.TwoFactorCode;
import com.pigs.voxly.domain.identity.enumerations.Role;
import com.pigs.voxly.domain.identity.events.*;
import com.pigs.voxly.domain.identity.valueobjects.Email;
import com.pigs.voxly.domain.identity.valueobjects.PasswordHash;
import com.pigs.voxly.domain.identity.valueobjects.RefreshToken;
import com.pigs.voxly.domain.identity.valueobjects.Username;
import com.pigs.voxly.sharedKernel.domain.ddd.AggregateRoot;
import com.pigs.voxly.sharedKernel.domain.results.Result;
import com.pigs.voxly.sharedKernel.domain.results.ResultT;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public final class User extends AggregateRoot<UserId> {

    // --- Identity ---
    private Email email;
    private Username username;
    private PasswordHash passwordHash;

    // --- Account state ---
    private boolean active;
    private Instant createdAt;
    private Instant modifiedAt;

    // --- Email verification ---
    private boolean emailVerified;
    private String emailVerificationToken;
    private Instant emailVerificationTokenExpiry;

    // --- Password reset ---
    private String passwordResetToken;
    private Instant passwordResetTokenExpiry;

    // --- Account lockout ---
    private int failedLoginAttempts;
    private Instant lockoutEnd;

    // --- Two-factor ---
    private boolean twoFactorEnabled;

    // --- Collections ---
    private final List<Role> roles = new ArrayList<>();
    private final List<RefreshToken> refreshTokens = new ArrayList<>();
    private final List<TwoFactorCode> twoFactorCodes = new ArrayList<>();

    // --- Constants ---
    private static final Duration EMAIL_TOKEN_VALIDITY = Duration.ofHours(24);
    private static final Duration PASSWORD_RESET_TOKEN_VALIDITY = Duration.ofHours(1);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private User() {
    }

    private User(UserId id, Email email, Username username, PasswordHash passwordHash) {
        super(id);
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.active = true;
        this.emailVerified = false;
        this.createdAt = Instant.now();
        this.twoFactorEnabled = false;
        this.failedLoginAttempts = 0;
        this.roles.add(Role.USER);

        this.emailVerificationToken = generateToken();
        this.emailVerificationTokenExpiry = Instant.now().plus(EMAIL_TOKEN_VALIDITY);
    }

    // ===== Reconstitution (from persistence) =====

    public static User reconstitute(UserId id, Email email, Username username, PasswordHash passwordHash,
                                    boolean active, Instant createdAt, Instant modifiedAt,
                                    boolean emailVerified, String emailVerificationToken,
                                    Instant emailVerificationTokenExpiry,
                                    String passwordResetToken, Instant passwordResetTokenExpiry,
                                    int failedLoginAttempts, Instant lockoutEnd,
                                    boolean twoFactorEnabled,
                                    List<Role> roles, List<RefreshToken> refreshTokens,
                                    List<TwoFactorCode> twoFactorCodes) {
        var user = new User();
        user.id = id;
        user.email = email;
        user.username = username;
        user.passwordHash = passwordHash;
        user.active = active;
        user.createdAt = createdAt;
        user.modifiedAt = modifiedAt;
        user.emailVerified = emailVerified;
        user.emailVerificationToken = emailVerificationToken;
        user.emailVerificationTokenExpiry = emailVerificationTokenExpiry;
        user.passwordResetToken = passwordResetToken;
        user.passwordResetTokenExpiry = passwordResetTokenExpiry;
        user.failedLoginAttempts = failedLoginAttempts;
        user.lockoutEnd = lockoutEnd;
        user.twoFactorEnabled = twoFactorEnabled;
        user.roles.addAll(roles);
        user.refreshTokens.addAll(refreshTokens);
        user.twoFactorCodes.addAll(twoFactorCodes);
        return user;
    }

    // ===== Factory =====

    public static ResultT<User> create(Email email, Username username, PasswordHash passwordHash) {
        var user = new User(UserId.create(), email, username, passwordHash);

        user.raiseDomainEvent(new UserRegisteredEvent(
                user.getId(),
                user.getEmail().getValue(),
                user.getUsername().getValue(),
                user.getEmailVerificationToken()));

        return ResultT.success(user);
    }

    // ===== Email Verification =====

    public Result verifyEmail(String token) {
        if (emailVerified) {
            return Result.success();
        }
        if (!token.equals(emailVerificationToken)) {
            return Result.failure(UserErrors.INVALID_VERIFICATION_TOKEN);
        }
        if (Instant.now().isAfter(emailVerificationTokenExpiry)) {
            return Result.failure(UserErrors.INVALID_VERIFICATION_TOKEN);
        }

        emailVerified = true;
        emailVerificationToken = null;
        emailVerificationTokenExpiry = null;
        markModified();

        raiseDomainEvent(new UserEmailVerifiedEvent(getId()));
        return Result.success();
    }

    public String regenerateEmailVerificationToken() {
        if (emailVerified) return null;

        emailVerificationToken = generateToken();
        emailVerificationTokenExpiry = Instant.now().plus(EMAIL_TOKEN_VALIDITY);
        markModified();
        return emailVerificationToken;
    }

    // ===== Password =====

    public void changePassword(PasswordHash newPasswordHash) {
        this.passwordHash = newPasswordHash;
        markModified();
        raiseDomainEvent(new UserPasswordChangedEvent(getId()));
    }

    public String requestPasswordReset() {
        passwordResetToken = generateToken();
        passwordResetTokenExpiry = Instant.now().plus(PASSWORD_RESET_TOKEN_VALIDITY);
        markModified();

        raiseDomainEvent(new PasswordResetRequestedEvent(
                getId(), email.getValue(), username.getValue(), passwordResetToken));

        return passwordResetToken;
    }

    public Result resetPassword(String token, PasswordHash newPasswordHash) {
        if (!token.equals(passwordResetToken)) {
            return Result.failure(UserErrors.INVALID_PASSWORD_RESET_TOKEN);
        }
        if (passwordResetTokenExpiry == null || Instant.now().isAfter(passwordResetTokenExpiry)) {
            return Result.failure(UserErrors.PASSWORD_RESET_TOKEN_EXPIRED);
        }

        this.passwordHash = newPasswordHash;
        this.passwordResetToken = null;
        this.passwordResetTokenExpiry = null;
        markModified();

        raiseDomainEvent(new UserPasswordChangedEvent(getId()));
        return Result.success();
    }

    // ===== Account Lockout =====

    public boolean isLockedOut() {
        return lockoutEnd != null && Instant.now().isBefore(lockoutEnd);
    }

    public void recordFailedLogin() {
        failedLoginAttempts++;

        if (failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            lockoutEnd = Instant.now().plus(LOCKOUT_DURATION);
            markModified();
            raiseDomainEvent(new UserLockedOutEvent(getId(), lockoutEnd, failedLoginAttempts));
        } else {
            markModified();
        }
    }

    public void recordSuccessfulLogin() {
        failedLoginAttempts = 0;
        lockoutEnd = null;
        markModified();
    }

    // ===== Two-Factor Authentication =====

    public Result enableTwoFactor() {
        if (twoFactorEnabled) {
            return Result.failure(UserErrors.TWO_FACTOR_ALREADY_ENABLED);
        }
        twoFactorEnabled = true;
        markModified();
        raiseDomainEvent(new TwoFactorToggledEvent(getId(), true));
        return Result.success();
    }

    public Result disableTwoFactor() {
        if (!twoFactorEnabled) {
            return Result.failure(UserErrors.TWO_FACTOR_NOT_ENABLED);
        }
        twoFactorEnabled = false;
        markModified();
        raiseDomainEvent(new TwoFactorToggledEvent(getId(), false));
        return Result.success();
    }

    public TwoFactorCode generateTwoFactorCode() {
        twoFactorCodes.stream()
                .filter(c -> !c.isUsed())
                .forEach(TwoFactorCode::markAsUsed);

        var code = TwoFactorCode.generate();
        twoFactorCodes.add(code);
        markModified();

        raiseDomainEvent(new TwoFactorCodeGeneratedEvent(
                getId(), email.getValue(), username.getValue(), code.getCode()));

        return code;
    }

    public Result validateTwoFactorCode(String code) {
        var latest = twoFactorCodes.stream()
                .filter(c -> !c.isUsed())
                .reduce((first, second) -> second)
                .orElse(null);

        if (latest == null) {
            return Result.failure(UserErrors.INVALID_TWO_FACTOR_CODE);
        }
        if (latest.isExpired()) {
            return Result.failure(UserErrors.TWO_FACTOR_CODE_EXPIRED);
        }
        if (!latest.validate(code)) {
            return Result.failure(UserErrors.INVALID_TWO_FACTOR_CODE);
        }

        latest.markAsUsed();
        markModified();
        return Result.success();
    }

    // ===== Roles =====

    public Result addRole(Role role) {
        if (roles.contains(role)) {
            return Result.failure(UserErrors.ROLE_ALREADY_ASSIGNED);
        }
        roles.add(role);
        markModified();
        raiseDomainEvent(new UserRoleChangedEvent(getId(), role, UserRoleChangedEvent.Action.ADDED));
        return Result.success();
    }

    public Result removeRole(Role role) {
        if (!roles.contains(role)) {
            return Result.failure(UserErrors.ROLE_NOT_ASSIGNED);
        }
        roles.remove(role);
        markModified();
        raiseDomainEvent(new UserRoleChangedEvent(getId(), role, UserRoleChangedEvent.Action.REMOVED));
        return Result.success();
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    // ===== Refresh Tokens =====

    public void addRefreshToken(RefreshToken token) {
        refreshTokens.removeIf(t -> !t.isActive());
        refreshTokens.add(token);
        markModified();
    }

    public Result revokeRefreshToken(String token, String replacedByToken) {
        for (int i = 0; i < refreshTokens.size(); i++) {
            if (refreshTokens.get(i).getToken().equals(token)) {
                refreshTokens.set(i, refreshTokens.get(i).revoke(replacedByToken));
                markModified();
                return Result.success();
            }
        }
        return Result.failure(UserErrors.REFRESH_TOKEN_NOT_FOUND);
    }

    public Result revokeRefreshToken(String token) {
        return revokeRefreshToken(token, null);
    }

    public void revokeAllRefreshTokens() {
        for (int i = 0; i < refreshTokens.size(); i++) {
            if (refreshTokens.get(i).isActive()) {
                refreshTokens.set(i, refreshTokens.get(i).revoke());
            }
        }
        markModified();
    }

    public RefreshToken findRefreshToken(String token) {
        return refreshTokens.stream()
                .filter(t -> t.getToken().equals(token))
                .findFirst()
                .orElse(null);
    }

    // ===== Account State =====

    public void deactivate() {
        if (!active) return;
        active = false;
        revokeAllRefreshTokens();
        markModified();
        raiseDomainEvent(new UserDeactivatedEvent(getId()));
    }

    public void activate() {
        if (active) return;
        active = true;
        markModified();
    }

    // ===== Profile Update =====

    public Result updateUsername(Username newUsername) {
        if (this.username.equals(newUsername)) {
            return Result.success();
        }
        this.username = newUsername;
        markModified();
        return Result.success();
    }

    // ===== Getters =====

    public Email getEmail() {
        return email;
    }

    public Username getUsername() {
        return username;
    }

    public PasswordHash getPasswordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public String getEmailVerificationToken() {
        return emailVerificationToken;
    }

    public Instant getEmailVerificationTokenExpiry() {
        return emailVerificationTokenExpiry;
    }

    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public Instant getPasswordResetTokenExpiry() {
        return passwordResetTokenExpiry;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockoutEnd() {
        return lockoutEnd;
    }

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getModifiedAt() {
        return modifiedAt;
    }

    public List<Role> getRoles() {
        return Collections.unmodifiableList(roles);
    }

    public List<RefreshToken> getRefreshTokens() {
        return Collections.unmodifiableList(refreshTokens);
    }

    public List<TwoFactorCode> getTwoFactorCodes() {
        return Collections.unmodifiableList(twoFactorCodes);
    }

    // ===== Helpers =====

    private void markModified() {
        this.modifiedAt = Instant.now();
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
