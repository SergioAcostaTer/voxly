package com.pigs.voxly.api.identity;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pigs.voxly.api.identity.cookie.AccessTokenCookieHelper;
import com.pigs.voxly.api.identity.dto.AccessTokenResponse;
import com.pigs.voxly.api.identity.dto.LoginApiResponse;
import com.pigs.voxly.api.identity.dto.RequestPasswordResetRequest;
import com.pigs.voxly.api.identity.dto.RequestTwoFactorCodeRequest;
import com.pigs.voxly.api.identity.dto.ResetPasswordRequest;
import com.pigs.voxly.api.shared.ApiResponse;
import com.pigs.voxly.api.shared.ResultMapper;
import com.pigs.voxly.application.identity.AuthService;
import com.pigs.voxly.application.identity.dto.LoginRequest;
import com.pigs.voxly.application.identity.dto.RegisterRequest;
import com.pigs.voxly.application.identity.dto.UserResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Authentication", description = "Register, login, logout, token refresh, email verification, password reset, and 2FA")
public class AuthController {

    private final AuthService authService;
    private final AccessTokenCookieHelper cookieHelper;

    public AuthController(AuthService authService, AccessTokenCookieHelper cookieHelper) {
        this.authService = authService;
        this.cookieHelper = cookieHelper;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody RegisterRequest request) {
        return ResultMapper.toCreatedResponse(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email/username and password", description = "Returns access token in body and access token cookie. If 2FA is required, no tokens are issued.")
    public ResponseEntity<ApiResponse<LoginApiResponse>> login(@RequestBody LoginRequest request) {
        var result = authService.login(request);

        if (result.isFailure()) {
            return ResultMapper.toResponse(result.map(r -> (LoginApiResponse) null));
        }

        var loginResponse = result.getValue();
        var apiResponse = LoginApiResponse.from(loginResponse);

        if (loginResponse.requiresTwoFactor()) {
            return ResponseEntity.ok(ApiResponse.ok(apiResponse));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        cookieHelper.createCookie(loginResponse.tokens().refreshToken()).toString())
                .body(ApiResponse.ok(apiResponse));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout by revoking cookie token")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "access_token", required = false) String accessTokenCookie) {

        if (accessTokenCookie == null || accessTokenCookie.isBlank()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookieHelper.clearCookie().toString())
                    .body(ApiResponse.ok());
        }

        var result = authService.logout(accessTokenCookie);

        return ResponseEntity.status(result.isSuccess() ? HttpStatus.OK : HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, cookieHelper.clearCookie().toString())
                .body(ApiResponse.ok());
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Issue a new access token using the cookie token")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> issueAccessToken(
            @CookieValue(name = "access_token", required = false) String accessTokenCookie) {

        if (accessTokenCookie == null || accessTokenCookie.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH.ACCESS_TOKEN_MISSING", "No access token cookie provided."));
        }

        var result = authService.refreshToken(accessTokenCookie);

        if (result.isFailure()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, cookieHelper.clearCookie().toString())
                    .body(ResultMapper.extractErrors(result));
        }

        var tokens = result.getValue();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieHelper.createCookie(tokens.refreshToken()).toString())
                .body(ApiResponse.ok(AccessTokenResponse.from(tokens)));
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify email address via token link")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        return ResultMapper.toResponse(authService.verifyEmailByToken(token));
    }

    @PostMapping("/request-password-reset")
    @Operation(summary = "Request a password reset email")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(@RequestBody RequestPasswordResetRequest request) {
        return ResultMapper.toResponse(authService.requestPasswordReset(request.email()));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using a valid reset token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody ResetPasswordRequest request) {
        return ResultMapper.toResponse(authService.resetPassword(request.token(), request.newPassword()));
    }

    @PostMapping("/request-two-factor-code")
    @Operation(summary = "Request a 2FA code to be sent via email")
    public ResponseEntity<ApiResponse<Void>> requestTwoFactorCode(@RequestBody RequestTwoFactorCodeRequest request) {
        return ResultMapper.toResponse(authService.requestTwoFactorCode(request.identifier(), request.password()));
    }
}
