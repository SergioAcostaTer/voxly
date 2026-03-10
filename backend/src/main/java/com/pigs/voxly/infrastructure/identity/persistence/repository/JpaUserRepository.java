package com.pigs.voxly.infrastructure.identity.persistence.repository;

import com.pigs.voxly.domain.identity.User;
import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.domain.identity.UserRepository;
import com.pigs.voxly.domain.identity.valueobjects.Email;
import com.pigs.voxly.domain.identity.valueobjects.Username;
import com.pigs.voxly.infrastructure.identity.persistence.mapper.UserPersistenceMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaUserRepository implements UserRepository {

    private final SpringDataUserRepository springDataRepo;
    private final UserPersistenceMapper mapper;

    public JpaUserRepository(SpringDataUserRepository springDataRepo, UserPersistenceMapper mapper) {
        this.springDataRepo = springDataRepo;
        this.mapper = mapper;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return springDataRepo.findById(id.getValue()).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return springDataRepo.findByEmail(email.getValue()).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        return springDataRepo.findByUsername(username.getValue()).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmailOrUsername(String identifier) {
        return springDataRepo.findByEmailOrUsername(identifier).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return springDataRepo.existsByEmail(email.getValue());
    }

    @Override
    public boolean existsByUsername(Username username) {
        return springDataRepo.existsByUsername(username.getValue());
    }

    @Override
    public Optional<User> findByRefreshToken(String refreshToken) {
        return springDataRepo.findByRefreshToken(refreshToken).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmailVerificationToken(String token) {
        return springDataRepo.findByEmailVerificationToken(token).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByPasswordResetToken(String token) {
        return springDataRepo.findByPasswordResetToken(token).map(mapper::toDomain);
    }

    @Override
    public List<User> findByIds(List<UUID> ids) {
        return springDataRepo.findAllById(ids).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void save(User user) {
        springDataRepo.save(mapper.toJpa(user));
    }

    @Override
    public void delete(User user) {
        springDataRepo.deleteById(user.getId().getValue());
    }
}
