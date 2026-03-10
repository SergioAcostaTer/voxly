package com.pigs.voxly.application.identity;

import com.pigs.voxly.application.identity.dto.UserResponse;
import com.pigs.voxly.application.identity.ports.CurrentUserProvider;
import com.pigs.voxly.domain.identity.UserErrors;
import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.domain.identity.UserRepository;
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
}
