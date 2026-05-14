package com.pigs.voxly.domain.identity.enumerations;

public enum ExperienceLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED;

    public static ExperienceLevel fromString(String value) {
        if (value == null) return null;
        try {
            return ExperienceLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
