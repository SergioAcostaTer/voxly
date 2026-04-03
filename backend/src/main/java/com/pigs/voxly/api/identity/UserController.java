package com.pigs.voxly.api.identity;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pigs.voxly.api.shared.ApiResponse;
import com.pigs.voxly.api.shared.ResultMapper;
import com.pigs.voxly.application.identity.AuthService;
import com.pigs.voxly.application.identity.UserService;
import com.pigs.voxly.application.identity.dto.UpdateProfileRequest;
import com.pigs.voxly.application.identity.dto.UserResponse;
import com.pigs.voxly.application.identity.ports.CurrentUserProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/users")
@Tag(name = "Users", description = "User profile and account management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final CurrentUserProvider currentUserProvider;

    public UserController(UserService userService, AuthService authService, CurrentUserProvider currentUserProvider) {
        this.userService = userService;
        this.authService = authService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        return ResultMapper.toResponse(userService.getCurrentUser());
    }

    @PatchMapping("/me")
    @Operation(summary = "Update the current user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = currentUserProvider.getUserId().orElseThrow();
        return ResultMapper.toResponse(userService.updateProfile(userId, request));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Deactivate the current user's account")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount() {
        UUID userId = currentUserProvider.getUserId().orElseThrow();
        return ResultMapper.toResponse(userService.deactivateAccount(userId));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get a user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID userId) {
        return ResultMapper.toResponse(userService.getUserById(userId));
    }

    @PostMapping("/me/enable-two-factor")
    @Operation(summary = "Enable two-factor authentication for the current user")
    public ResponseEntity<ApiResponse<Void>> enableTwoFactor() {
        UUID userId = currentUserProvider.getUserId().orElseThrow();
        return ResultMapper.toResponse(authService.enableTwoFactor(userId));
    }

    @PostMapping("/me/disable-two-factor")
    @Operation(summary = "Disable two-factor authentication for the current user")
    public ResponseEntity<ApiResponse<Void>> disableTwoFactor() {
        UUID userId = currentUserProvider.getUserId().orElseThrow();
        return ResultMapper.toResponse(authService.disableTwoFactor(userId));
    }
}
