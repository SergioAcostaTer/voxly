package com.pigs.voxly.infrastructure.identity.persistence.mapper;

import com.pigs.voxly.domain.identity.User;
import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.domain.identity.entities.TwoFactorCode;
import com.pigs.voxly.domain.identity.enumerations.Role;
import com.pigs.voxly.domain.identity.valueobjects.Email;
import com.pigs.voxly.domain.identity.valueobjects.PasswordHash;
import com.pigs.voxly.domain.identity.valueobjects.RefreshToken;
import com.pigs.voxly.domain.identity.valueobjects.Username;
import com.pigs.voxly.infrastructure.identity.persistence.entity.RefreshTokenJpaEntity;
import com.pigs.voxly.infrastructure.identity.persistence.entity.TwoFactorCodeJpaEntity;
import com.pigs.voxly.infrastructure.identity.persistence.entity.UserJpaEntity;
import com.pigs.voxly.sharedKernel.domain.types.Enumeration;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserPersistenceMapper {

    public User toDomain(UserJpaEntity entity) {
        List<Role> roles = entity.getRoles().stream()
                .map(name -> Enumeration.fromName(Role.class, name)
                        .orElseThrow(() -> new IllegalStateException("Unknown role: " + name)))
                .toList();

        List<RefreshToken> refreshTokens = entity.getRefreshTokens().stream()
                .map(rt -> RefreshToken.reconstitute(
                        rt.getToken(), rt.getExpiresAt(), rt.getCreatedAt(),
                        rt.isRevoked(), rt.getRevokedAt(), rt.getReplacedByToken()))
                .toList();

        List<TwoFactorCode> twoFactorCodes = entity.getTwoFactorCodes().stream()
                .map(tfc -> TwoFactorCode.reconstitute(
                        tfc.getId(), tfc.getCode(), tfc.getCreatedAt(),
                        tfc.getExpiresAt(), tfc.isUsed(), tfc.getUsedAt()))
                .toList();

        return User.reconstitute(
                UserId.from(entity.getId()),
                Email.reconstitute(entity.getEmail()),
                Username.reconstitute(entity.getUsername()),
                PasswordHash.reconstitute(entity.getPasswordHash()),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getModifiedAt(),
                entity.isEmailVerified(),
                entity.getEmailVerificationToken(),
                entity.getEmailVerificationTokenExpiry(),
                entity.getPasswordResetToken(),
                entity.getPasswordResetTokenExpiry(),
                entity.getFailedLoginAttempts(),
                entity.getLockoutEnd(),
                entity.isTwoFactorEnabled(),
                roles,
                refreshTokens,
                twoFactorCodes);
    }

    public UserJpaEntity toJpa(User user) {
        var entity = new UserJpaEntity();
        entity.setId(user.getId().getValue());
        entity.setEmail(user.getEmail().getValue());
        entity.setUsername(user.getUsername().getValue());
        entity.setPasswordHash(user.getPasswordHash().getValue());
        entity.setActive(user.isActive());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setModifiedAt(user.getModifiedAt());
        entity.setEmailVerified(user.isEmailVerified());
        entity.setEmailVerificationToken(user.getEmailVerificationToken());
        entity.setEmailVerificationTokenExpiry(user.getEmailVerificationTokenExpiry());
        entity.setPasswordResetToken(user.getPasswordResetToken());
        entity.setPasswordResetTokenExpiry(user.getPasswordResetTokenExpiry());
        entity.setFailedLoginAttempts(user.getFailedLoginAttempts());
        entity.setLockoutEnd(user.getLockoutEnd());
        entity.setTwoFactorEnabled(user.isTwoFactorEnabled());

        entity.setRoles(user.getRoles().stream()
                .map(Role::getName)
                .toList());

        entity.setRefreshTokens(user.getRefreshTokens().stream()
                .map(rt -> toRefreshTokenJpa(rt, entity))
                .toList());

        entity.setTwoFactorCodes(user.getTwoFactorCodes().stream()
                .map(tfc -> toTwoFactorCodeJpa(tfc, entity))
                .toList());

        return entity;
    }

    private RefreshTokenJpaEntity toRefreshTokenJpa(RefreshToken rt, UserJpaEntity userEntity) {
        var entity = new RefreshTokenJpaEntity();
        entity.setUser(userEntity);
        entity.setToken(rt.getToken());
        entity.setExpiresAt(rt.getExpiresAt());
        entity.setCreatedAt(rt.getCreatedAt());
        entity.setRevoked(rt.isRevoked());
        entity.setRevokedAt(rt.getRevokedAt());
        entity.setReplacedByToken(rt.getReplacedByToken());
        return entity;
    }

    private TwoFactorCodeJpaEntity toTwoFactorCodeJpa(TwoFactorCode tfc, UserJpaEntity userEntity) {
        var entity = new TwoFactorCodeJpaEntity();
        entity.setId(tfc.getId());
        entity.setUser(userEntity);
        entity.setCode(tfc.getCode());
        entity.setCreatedAt(tfc.getCreatedAt());
        entity.setExpiresAt(tfc.getExpiresAt());
        entity.setUsed(tfc.isUsed());
        entity.setUsedAt(tfc.getUsedAt());
        return entity;
    }
}
