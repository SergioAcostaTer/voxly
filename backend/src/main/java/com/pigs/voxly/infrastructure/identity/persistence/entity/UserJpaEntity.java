package com.pigs.voxly.infrastructure.identity.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "email", length = 256, nullable = false, unique = true)
    private String email;

    @Column(name = "username", length = 50, nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", length = 256, nullable = false)
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "email_verification_token", length = 100)
    private String emailVerificationToken;

    @Column(name = "email_verification_token_expiry")
    private Instant emailVerificationTokenExpiry;

    @Column(name = "password_reset_token", length = 100)
    private String passwordResetToken;

    @Column(name = "password_reset_token_expiry")
    private Instant passwordResetTokenExpiry;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "lockout_end")
    private Instant lockoutEnd;

    @Column(name = "two_factor_enabled", nullable = false)
    private boolean twoFactorEnabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "modified_at")
    private Instant modifiedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role_name", length = 50)
    private List<String> roles = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RefreshTokenJpaEntity> refreshTokens = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TwoFactorCodeJpaEntity> twoFactorCodes = new ArrayList<>();

    public UserJpaEntity() {
    }

    // --- Getters and setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getEmailVerificationToken() { return emailVerificationToken; }
    public void setEmailVerificationToken(String emailVerificationToken) { this.emailVerificationToken = emailVerificationToken; }

    public Instant getEmailVerificationTokenExpiry() { return emailVerificationTokenExpiry; }
    public void setEmailVerificationTokenExpiry(Instant emailVerificationTokenExpiry) { this.emailVerificationTokenExpiry = emailVerificationTokenExpiry; }

    public String getPasswordResetToken() { return passwordResetToken; }
    public void setPasswordResetToken(String passwordResetToken) { this.passwordResetToken = passwordResetToken; }

    public Instant getPasswordResetTokenExpiry() { return passwordResetTokenExpiry; }
    public void setPasswordResetTokenExpiry(Instant passwordResetTokenExpiry) { this.passwordResetTokenExpiry = passwordResetTokenExpiry; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public Instant getLockoutEnd() { return lockoutEnd; }
    public void setLockoutEnd(Instant lockoutEnd) { this.lockoutEnd = lockoutEnd; }

    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(Instant modifiedAt) { this.modifiedAt = modifiedAt; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public List<RefreshTokenJpaEntity> getRefreshTokens() { return refreshTokens; }
    public void setRefreshTokens(List<RefreshTokenJpaEntity> refreshTokens) { this.refreshTokens = refreshTokens; }

    public List<TwoFactorCodeJpaEntity> getTwoFactorCodes() { return twoFactorCodes; }
    public void setTwoFactorCodes(List<TwoFactorCodeJpaEntity> twoFactorCodes) { this.twoFactorCodes = twoFactorCodes; }
}
