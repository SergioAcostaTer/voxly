package com.pigs.voxly.application.identity;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pigs.voxly.application.identity.dto.AuthTokensResponse;
import com.pigs.voxly.application.identity.dto.LoginRequest;
import com.pigs.voxly.application.identity.dto.LoginResponse;
import com.pigs.voxly.application.identity.dto.RegisterRequest;
import com.pigs.voxly.application.identity.dto.UserResponse;
import com.pigs.voxly.application.identity.ports.IdentitySettingsRepository;
import com.pigs.voxly.application.identity.ports.JwtTokenProvider;
import com.pigs.voxly.application.identity.ports.PasswordHasher;
import com.pigs.voxly.domain.identity.User;
import com.pigs.voxly.domain.identity.UserErrors;
import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.domain.identity.UserRepository;
import com.pigs.voxly.domain.identity.valueobjects.Email;
import com.pigs.voxly.domain.identity.valueobjects.PasswordHash;
import com.pigs.voxly.domain.identity.valueobjects.RefreshToken;
import com.pigs.voxly.domain.identity.valueobjects.Username;
import com.pigs.voxly.sharedKernel.domain.events.DomainEventPublisher;
import com.pigs.voxly.sharedKernel.domain.results.Result;
import com.pigs.voxly.sharedKernel.domain.results.ResultT;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final IdentitySettingsRepository identitySettingsRepository;
    private final PasswordHasher passwordHasher;
    private final JwtTokenProvider jwtTokenProvider;
    private final DomainEventPublisher domainEventPublisher;
    private final boolean devBypassEnabled;
    private final String devEmail;
    private final String devPassword;

    public AuthService(UserRepository userRepository,
            IdentitySettingsRepository identitySettingsRepository,
            PasswordHasher passwordHasher,
            JwtTokenProvider jwtTokenProvider,
            DomainEventPublisher domainEventPublisher,
            @org.springframework.beans.factory.annotation.Value("${app.auth.dev-bypass-enabled:false}") boolean devBypassEnabled,
            @org.springframework.beans.factory.annotation.Value("${app.auth.dev-email:}") String devEmail,
            @org.springframework.beans.factory.annotation.Value("${app.auth.dev-password:}") String devPassword) {
        this.userRepository = userRepository;
        this.identitySettingsRepository = identitySettingsRepository;
        this.passwordHasher = passwordHasher;
        this.jwtTokenProvider = jwtTokenProvider;
        this.domainEventPublisher = domainEventPublisher;
        this.devBypassEnabled = devBypassEnabled;
        this.devEmail = devEmail;
        this.devPassword = devPassword;
    }

    // ===== Register =====

    @Transactional
    public ResultT<UserResponse> register(RegisterRequest request) {
        var emailResult = Email.create(request.email());
        if (emailResult.isFailure())
            return ResultT.failure(emailResult.getErrors());

        var usernameResult = Username.create(request.username());
        if (usernameResult.isFailure())
            return ResultT.failure(usernameResult.getErrors());

        var email = emailResult.getValue();
        var username = usernameResult.getValue();

        if (userRepository.existsByEmail(email)) {
            return ResultT.failure(UserErrors.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByUsername(username)) {
            return ResultT.failure(UserErrors.USERNAME_ALREADY_EXISTS);
        }

        String hashedPassword = passwordHasher.hash(request.password());
        var passwordHashResult = PasswordHash.create(hashedPassword);
        if (passwordHashResult.isFailure())
            return ResultT.failure(passwordHashResult.getErrors());

        var userResult = User.create(email, username, passwordHashResult.getValue());
        if (userResult.isFailure())
            return ResultT.failure(userResult.getErrors());

        boolean requiresEmailVerification = identitySettingsRepository.isEmailVerificationRequired();
        var user = userResult.getValue();

        // If verification is disabled, mark account verified at signup.
        if (!requiresEmailVerification && user.getEmailVerificationToken() != null) {
            user.verifyEmail(user.getEmailVerificationToken());
        }

        userRepository.save(user);
        publishAndClear(user);

        return ResultT.success(UserResponse.fromDomain(user));
    }

    // ===== Login =====

    @Transactional
    public ResultT<LoginResponse> login(LoginRequest request) {
        var userOpt = userRepository.findByEmailOrUsername(request.identifier());

        // DEV BYPASS: Auto-create dev user if doesn't exist
        if (devBypassEnabled && hasDevBypassConfigured() && userOpt.isEmpty()
                && request.identifier().equalsIgnoreCase(devEmail)) {
            var devUser = createDevUser();
            if (devUser != null) {
                userOpt = java.util.Optional.of(devUser);
            }
        }

        if (userOpt.isEmpty()) {
            return ResultT.failure(UserErrors.INVALID_CREDENTIALS);
        }

        var user = userOpt.get();

        // DEV BYPASS: Skip all validations for dev email
        boolean isDevUser = devBypassEnabled && hasDevBypassConfigured()
                && user.getEmail().getValue().equalsIgnoreCase(devEmail);

        if (!isDevUser) {
            boolean requiresEmailVerification = identitySettingsRepository.isEmailVerificationRequired();

            if (!user.isActive()) {
                return ResultT.failure(UserErrors.USER_DEACTIVATED);
            }
            if (user.isLockedOut()) {
                return ResultT.failure(UserErrors.accountLockedUntil(user.getLockoutEnd()));
            }
            if (!passwordHasher.verify(request.password(), user.getPasswordHash().getValue())) {
                user.recordFailedLogin();
                userRepository.save(user);
                publishAndClear(user);
                return ResultT.failure(UserErrors.INVALID_CREDENTIALS);
            }
            if (requiresEmailVerification && !user.isEmailVerified()) {
                return ResultT.failure(UserErrors.EMAIL_NOT_VERIFIED);
            }

            // Two-factor flow
            if (user.isTwoFactorEnabled()) {
                if (!request.hasTwoFactorCode()) {
                    user.generateTwoFactorCode();
                    userRepository.save(user);
                    publishAndClear(user);
                    return ResultT.success(LoginResponse.twoFactorRequired(UserResponse.fromDomain(user)));
                }

                var codeResult = user.validateTwoFactorCode(request.twoFactorCode());
                if (codeResult.isFailure()) {
                    userRepository.save(user);
                    return ResultT.failure(codeResult.getErrors());
                }
            }
        }

        user.recordSuccessfulLogin();
        var tokens = generateAndStoreTokens(user);
        userRepository.save(user);
        publishAndClear(user);

        return ResultT.success(LoginResponse.success(UserResponse.fromDomain(user), tokens));
    }

    // ===== Logout =====

    @Transactional
    public Result logout(String refreshTokenValue) {
        var userOpt = userRepository.findByRefreshToken(refreshTokenValue);
        if (userOpt.isEmpty()) {
            return Result.failure(UserErrors.REFRESH_TOKEN_NOT_FOUND);
        }

        var user = userOpt.get();
        var result = user.revokeRefreshToken(refreshTokenValue);
        if (result.isFailure())
            return result;

        userRepository.save(user);
        return Result.success();
    }

    // ===== Refresh Token =====

    @Transactional
    public ResultT<AuthTokensResponse> refreshToken(String refreshTokenValue) {
        var userOpt = userRepository.findByRefreshToken(refreshTokenValue);
        if (userOpt.isEmpty()) {
            return ResultT.failure(UserErrors.REFRESH_TOKEN_NOT_FOUND);
        }

        var user = userOpt.get();
        var existingToken = user.findRefreshToken(refreshTokenValue);

        if (existingToken == null) {
            return ResultT.failure(UserErrors.REFRESH_TOKEN_NOT_FOUND);
        }
        if (existingToken.isRevoked()) {
            user.revokeAllRefreshTokens();
            userRepository.save(user);
            return ResultT.failure(UserErrors.REFRESH_TOKEN_REVOKED);
        }
        if (existingToken.isExpired()) {
            return ResultT.failure(UserErrors.REFRESH_TOKEN_EXPIRED);
        }

        var newTokens = generateAndStoreTokens(user);
        user.revokeRefreshToken(refreshTokenValue, newTokens.refreshToken());
        userRepository.save(user);

        return ResultT.success(newTokens);
    }

    // ===== Email Verification =====

    @Transactional
    public Result verifyEmail(UUID userId, String token) {
        var userOpt = userRepository.findById(UserId.from(userId));
        if (userOpt.isEmpty()) {
            return Result.failure(UserErrors.USER_NOT_FOUND);
        }

        var user = userOpt.get();
        var result = user.verifyEmail(token);
        if (result.isFailure())
            return result;

        userRepository.save(user);
        publishAndClear(user);
        return Result.success();
    }

    @Transactional
    public Result verifyEmailByToken(String token) {
        var userOpt = userRepository.findByEmailVerificationToken(token);
        if (userOpt.isEmpty()) {
            return Result.failure(UserErrors.INVALID_VERIFICATION_TOKEN);
        }

        var user = userOpt.get();
        var result = user.verifyEmail(token);
        if (result.isFailure())
            return result;

        userRepository.save(user);
        publishAndClear(user);
        return Result.success();
    }

    // ===== Password Reset =====

    @Transactional
    public Result requestPasswordReset(String emailValue) {
        var emailResult = Email.create(emailValue);
        if (emailResult.isFailure())
            return emailResult.toResult();

        var userOpt = userRepository.findByEmail(emailResult.getValue());
        if (userOpt.isEmpty()) {
            return Result.failure(UserErrors.userNotFoundByEmail(emailValue));
        }

        var user = userOpt.get();
        if (!user.isActive()) {
            return Result.failure(UserErrors.USER_DEACTIVATED);
        }

        user.requestPasswordReset();
        userRepository.save(user);
        publishAndClear(user);

        return Result.success();
    }

    @Transactional
    public Result resetPassword(String token, String newPassword) {
        var userOpt = userRepository.findByPasswordResetToken(token);
        if (userOpt.isEmpty()) {
            return Result.failure(UserErrors.INVALID_PASSWORD_RESET_TOKEN);
        }

        var user = userOpt.get();
        String hashedPassword = passwordHasher.hash(newPassword);
        var passwordHashResult = PasswordHash.create(hashedPassword);
        if (passwordHashResult.isFailure())
            return passwordHashResult.toResult();

        var result = user.resetPassword(token, passwordHashResult.getValue());
        if (result.isFailure())
            return result;

        user.revokeAllRefreshTokens();
        userRepository.save(user);
        publishAndClear(user);

        return Result.success();
    }

    // ===== Two-Factor Authentication =====

    @Transactional
    public Result enableTwoFactor(UUID userId) {
        var userOpt = userRepository.findById(UserId.from(userId));
        if (userOpt.isEmpty()) {
            return Result.failure(UserErrors.USER_NOT_FOUND);
        }

        var user = userOpt.get();
        if (!user.isActive()) {
            return Result.failure(UserErrors.USER_DEACTIVATED);
        }

        var result = user.enableTwoFactor();
        if (result.isFailure())
            return result;

        userRepository.save(user);
        publishAndClear(user);
        return Result.success();
    }

    @Transactional
    public Result disableTwoFactor(UUID userId) {
        var userOpt = userRepository.findById(UserId.from(userId));
        if (userOpt.isEmpty()) {
            return Result.failure(UserErrors.USER_NOT_FOUND);
        }

        var user = userOpt.get();
        var result = user.disableTwoFactor();
        if (result.isFailure())
            return result;

        userRepository.save(user);
        publishAndClear(user);
        return Result.success();
    }

    @Transactional
    public Result requestTwoFactorCode(String identifier, String password) {
        var userOpt = userRepository.findByEmailOrUsername(identifier);
        if (userOpt.isEmpty()) {
            return Result.failure(UserErrors.INVALID_CREDENTIALS);
        }

        var user = userOpt.get();
        if (!passwordHasher.verify(password, user.getPasswordHash().getValue())) {
            return Result.failure(UserErrors.INVALID_CREDENTIALS);
        }
        if (!user.isTwoFactorEnabled()) {
            return Result.failure(UserErrors.TWO_FACTOR_NOT_ENABLED);
        }

        user.generateTwoFactorCode();
        userRepository.save(user);
        publishAndClear(user);

        return Result.success();
    }

    // ===== Helpers =====

    private User createDevUser() {
        try {
            if (!hasDevBypassConfigured()) {
                return null;
            }

            var emailResult = Email.create(devEmail);
            var usernameResult = Username.create("Eduardo");
            if (emailResult.isFailure() || usernameResult.isFailure())
                return null;

            String hashedPassword = passwordHasher.hash(devPassword);
            var passwordHashResult = PasswordHash.create(hashedPassword);
            if (passwordHashResult.isFailure())
                return null;

            var userResult = User.create(emailResult.getValue(), usernameResult.getValue(),
                    passwordHashResult.getValue());
            if (userResult.isFailure())
                return null;

            var user = userResult.getValue();
            // Force email verification for dev user
            user.verifyEmail(user.getEmailVerificationToken());
            userRepository.save(user);
            return user;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean hasDevBypassConfigured() {
        return devEmail != null && !devEmail.isBlank() && devPassword != null && !devPassword.isBlank();
    }

    private AuthTokensResponse generateAndStoreTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken();
        var accessExpiry = jwtTokenProvider.getAccessTokenExpiry();
        var refreshExpiry = jwtTokenProvider.getRefreshTokenExpiry();

        var refreshTokenResult = RefreshToken.create(refreshTokenValue, refreshExpiry);
        user.addRefreshToken(refreshTokenResult.getValue());

        return new AuthTokensResponse(accessToken, accessExpiry, refreshTokenValue, refreshExpiry);
    }

    private void publishAndClear(User user) {
        domainEventPublisher.publishAll(user.getDomainEvents());
        user.clearDomainEvents();
    }
}
