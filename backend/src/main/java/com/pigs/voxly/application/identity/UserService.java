package com.pigs.voxly.application.identity;

import com.pigs.voxly.application.identity.dto.UpdateProfileRequest;
import com.pigs.voxly.application.identity.dto.UserResponse;
import com.pigs.voxly.application.identity.ports.CurrentUserProvider;
import com.pigs.voxly.domain.identity.UserErrors;
import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.domain.identity.UserRepository;
import com.pigs.voxly.domain.identity.valueobjects.Username;
import com.pigs.voxly.sharedKernel.domain.results.Result;
import com.pigs.voxly.sharedKernel.domain.results.ResultT;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    public UserService(UserRepository userRepository, CurrentUserProvider currentUserProvider) {
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public ResultT<UserResponse> getUserById(UUID userId) {
        return userRepository.findById(UserId.from(userId))
                .map(user -> ResultT.success(UserResponse.fromDomain(user)))
                .orElseGet(() -> ResultT.failure(UserErrors.userNotFoundById(userId)));
    }

    public ResultT<UserResponse> getCurrentUser() {
        var userIdOpt = currentUserProvider.getUserId();
        if (userIdOpt.isEmpty()) {
            return ResultT.failure(UserErrors.USER_NOT_FOUND);
        }
        return getUserById(userIdOpt.get());
    }

    @Transactional
    public ResultT<UserResponse> updateProfile(UUID userId, UpdateProfileRequest request) {
        var userOpt = userRepository.findById(UserId.from(userId));
        if (userOpt.isEmpty()) {
            return ResultT.failure(UserErrors.userNotFoundById(userId));
        }

        var user = userOpt.get();

        if (request.username() != null && !request.username().isBlank()) {
            var usernameResult = Username.create(request.username());
            if (usernameResult.isFailure()) {
                return ResultT.failure(usernameResult.getError());
            }

            // Check if username is already taken by another user
            var existingUser = userRepository.findByUsername(usernameResult.getValue());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                return ResultT.failure(UserErrors.USERNAME_ALREADY_EXISTS);
            }

            var updateResult = user.updateUsername(usernameResult.getValue());
            if (updateResult.isFailure()) {
                return ResultT.failure(updateResult.getError());
            }
        }

        userRepository.save(user);
        return ResultT.success(UserResponse.fromDomain(user));
    }

    @Transactional
    public Result deactivateAccount(UUID userId) {
        var userOpt = userRepository.findById(UserId.from(userId));
        if (userOpt.isEmpty()) {
            return Result.failure(UserErrors.userNotFoundById(userId));
        }

        var user = userOpt.get();
        user.deactivate();
        userRepository.save(user);

        return Result.success();
    }
}
