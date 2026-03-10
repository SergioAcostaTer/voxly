package com.pigs.voxly.application.identity.ports;

public interface PasswordHasher {

    String hash(String rawPassword);

    boolean verify(String rawPassword, String hashedPassword);
}
