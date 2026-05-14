package com.pigs.voxly.application.identity.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CurrentUserProvider {

    Optional<UUID> getUserId();

    Optional<String> getEmail();

    Optional<String> getUsername();

    List<String> getRoles();

    boolean isAuthenticated();

    boolean isInRole(String role);
}
