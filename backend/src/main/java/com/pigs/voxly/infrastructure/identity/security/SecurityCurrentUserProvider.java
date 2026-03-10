package com.pigs.voxly.infrastructure.identity.security;

import com.pigs.voxly.application.identity.ports.CurrentUserProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SecurityCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Optional<UUID> getUserId() {
        return getJwt().map(jwt -> UUID.fromString(jwt.getSubject()));
    }

    @Override
    public Optional<String> getEmail() {
        return getJwt().map(jwt -> jwt.getClaimAsString("email"));
    }

    @Override
    public Optional<String> getUsername() {
        return getJwt().map(jwt -> jwt.getClaimAsString("username"));
    }

    @Override
    public List<String> getRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return List.of();
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    @Override
    public boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
    }

    @Override
    public boolean isInRole(String role) {
        return getRoles().contains(role);
    }

    private Optional<Jwt> getJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt);
        }
        return Optional.empty();
    }
}
