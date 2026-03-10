package com.pigs.voxly.domain.identity;

import com.pigs.voxly.domain.identity.valueobjects.Email;
import com.pigs.voxly.domain.identity.valueobjects.Username;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(Email email);

    Optional<User> findByUsername(Username username);

    Optional<User> findByEmailOrUsername(String identifier);

    boolean existsByEmail(Email email);

    boolean existsByUsername(Username username);

    Optional<User> findByRefreshToken(String refreshToken);

    Optional<User> findByEmailVerificationToken(String token);

    Optional<User> findByPasswordResetToken(String token);

    List<User> findByIds(List<UUID> ids);

    void save(User user);

    void delete(User user);
}
