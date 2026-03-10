package com.pigs.voxly.domain.identity;

import com.pigs.voxly.sharedKernel.domain.results.Error;

import java.time.Instant;
import java.util.UUID;

public final class UserErrors {

    private UserErrors() {
    }

    // Email
    public static final Error EMAIL_REQUIRED = Error.validation(
            "User.EmailRequired", "Email is required.");

    public static Error emailTooLong(int maxLength) {
        return Error.validation(
                "User.EmailTooLong", "Email must not exceed %d characters.".formatted(maxLength));
    }

    public static final Error EMAIL_INVALID_FORMAT = Error.validation(
            "User.EmailInvalidFormat", "Email format is invalid.");

    public static final Error EMAIL_ALREADY_EXISTS = Error.conflict(
            "User.EmailAlreadyExists", "A user with this email already exists.");

    public static final Error EMAIL_NOT_VERIFIED = Error.failure(
            "User.EmailNotVerified", "Email address has not been verified.");

    // Username
    public static final Error USERNAME_REQUIRED = Error.validation(
            "User.UsernameRequired", "Username is required.");

    public static Error usernameTooShort(int minLength) {
        return Error.validation(
                "User.UsernameTooShort", "Username must be at least %d characters.".formatted(minLength));
    }

    public static Error usernameTooLong(int maxLength) {
        return Error.validation(
                "User.UsernameTooLong", "Username must not exceed %d characters.".formatted(maxLength));
    }

    public static final Error USERNAME_INVALID_FORMAT = Error.validation(
            "User.UsernameInvalidFormat", "Username must contain only alphanumeric characters.");

    public static final Error USERNAME_ALREADY_EXISTS = Error.conflict(
            "User.UsernameAlreadyExists", "A user with this username already exists.");

    // Password
    public static final Error PASSWORD_HASH_REQUIRED = Error.validation(
            "User.PasswordHashRequired", "Password hash is required.");

    public static final Error INVALID_CREDENTIALS = Error.unauthorized(
            "User.InvalidCredentials", "Invalid email/username or password.");

    public static final Error INVALID_PASSWORD_RESET_TOKEN = Error.validation(
            "User.InvalidPasswordResetToken", "Password reset token is invalid or expired.");

    public static final Error PASSWORD_RESET_TOKEN_EXPIRED = Error.validation(
            "User.PasswordResetTokenExpired", "Password reset token has expired.");

    // Email verification
    public static final Error INVALID_VERIFICATION_TOKEN = Error.validation(
            "User.InvalidVerificationToken", "Email verification token is invalid or expired.");

    // Refresh token
    public static final Error REFRESH_TOKEN_REQUIRED = Error.validation(
            "User.RefreshTokenRequired", "Refresh token is required.");

    public static final Error REFRESH_TOKEN_EXPIRY_IN_PAST = Error.validation(
            "User.RefreshTokenExpiryInPast", "Refresh token expiry must be in the future.");

    public static final Error REFRESH_TOKEN_NOT_FOUND = Error.notFound(
            "User.RefreshTokenNotFound", "Refresh token was not found.");

    public static final Error REFRESH_TOKEN_EXPIRED = Error.unauthorized(
            "User.RefreshTokenExpired", "Refresh token has expired.");

    public static final Error REFRESH_TOKEN_REVOKED = Error.unauthorized(
            "User.RefreshTokenRevoked", "Refresh token has been revoked.");

    // Two-factor
    public static final Error TWO_FACTOR_ALREADY_ENABLED = Error.conflict(
            "User.TwoFactorAlreadyEnabled", "Two-factor authentication is already enabled.");

    public static final Error TWO_FACTOR_NOT_ENABLED = Error.failure(
            "User.TwoFactorNotEnabled", "Two-factor authentication is not enabled.");

    public static final Error INVALID_TWO_FACTOR_CODE = Error.unauthorized(
            "User.InvalidTwoFactorCode", "Invalid two-factor authentication code.");

    public static final Error TWO_FACTOR_CODE_EXPIRED = Error.failure(
            "User.TwoFactorCodeExpired", "Two-factor authentication code has expired. Please request a new code.");

    public static final Error TWO_FACTOR_REQUIRED = Error.failure(
            "User.TwoFactorRequired", "Two-factor authentication code is required.");

    // Account state
    public static final Error ACCOUNT_LOCKED = Error.failure(
            "User.AccountLocked", "Account is temporarily locked due to too many failed login attempts.");

    public static Error accountLockedUntil(Instant lockoutEnd) {
        return Error.failure(
                "User.AccountLockedUntil", "Account is locked until %s.".formatted(lockoutEnd));
    }

    public static final Error USER_DEACTIVATED = Error.failure(
            "User.UserDeactivated", "User account has been deactivated.");

    public static final Error USER_NOT_FOUND = Error.notFound(
            "User.NotFound", "User was not found.");

    public static Error userNotFoundById(UUID id) {
        return Error.notFound(
                "User.NotFoundById", "User with ID '%s' was not found.".formatted(id));
    }

    public static Error userNotFoundByEmail(String email) {
        return Error.notFound(
                "User.NotFoundByEmail", "User with email '%s' was not found.".formatted(email));
    }

    // Roles
    public static final Error ROLE_ALREADY_ASSIGNED = Error.conflict(
            "User.RoleAlreadyAssigned", "User already has this role.");

    public static final Error ROLE_NOT_ASSIGNED = Error.failure(
            "User.RoleNotAssigned", "User does not have this role.");
}
