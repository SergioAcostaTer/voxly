package com.pigs.voxly.sharedKernel.domain.guards;

import com.pigs.voxly.sharedKernel.domain.exceptions.GuardException;

import java.util.Collection;
import java.util.UUID;

public final class Guard {

    private Guard() {
    }

    // --- Null checks ---

    public static <T> T againstNull(T value, String parameterName) {
        if (value == null) {
            throw GuardException.forParam(parameterName, "cannot be null");
        }
        return value;
    }

    // --- String checks ---

    public static String againstNullOrEmpty(String value, String parameterName) {
        if (value == null || value.isEmpty()) {
            throw GuardException.forParam(parameterName, "cannot be null or empty");
        }
        return value;
    }

    public static String againstNullOrBlank(String value, String parameterName) {
        if (value == null || value.isBlank()) {
            throw GuardException.forParam(parameterName, "cannot be null or blank");
        }
        return value;
    }

    public static String againstLength(String value, int maxLength, String parameterName) {
        againstNullOrEmpty(value, parameterName);
        if (value.length() > maxLength) {
            throw GuardException.forParam(parameterName, "must not exceed %d characters".formatted(maxLength));
        }
        return value;
    }

    public static String againstLengthRange(String value, int minLength, int maxLength, String parameterName) {
        againstNullOrEmpty(value, parameterName);
        if (value.length() < minLength || value.length() > maxLength) {
            throw GuardException.forParam(parameterName,
                    "must be between %d and %d characters".formatted(minLength, maxLength));
        }
        return value;
    }

    public static String againstPattern(String value, String regex, String parameterName) {
        againstNullOrEmpty(value, parameterName);
        if (!value.matches(regex)) {
            throw GuardException.forParam(parameterName, "has an invalid format");
        }
        return value;
    }

    // --- Number checks ---

    public static <T extends Comparable<T>> T againstNegative(T value, T zero, String parameterName) {
        againstNull(value, parameterName);
        if (value.compareTo(zero) < 0) {
            throw GuardException.forParam(parameterName, "cannot be negative");
        }
        return value;
    }

    public static <T extends Comparable<T>> T againstOutOfRange(T value, T min, T max, String parameterName) {
        againstNull(value, parameterName);
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw GuardException.forParam(parameterName, "must be between %s and %s".formatted(min, max));
        }
        return value;
    }

    public static int againstNegative(int value, String parameterName) {
        if (value < 0) {
            throw GuardException.forParam(parameterName, "cannot be negative");
        }
        return value;
    }

    public static long againstNegative(long value, String parameterName) {
        if (value < 0) {
            throw GuardException.forParam(parameterName, "cannot be negative");
        }
        return value;
    }

    public static int againstZeroOrNegative(int value, String parameterName) {
        if (value <= 0) {
            throw GuardException.forParam(parameterName, "must be positive");
        }
        return value;
    }

    // --- Collection checks ---

    public static <T> Collection<T> againstNullOrEmpty(Collection<T> value, String parameterName) {
        if (value == null || value.isEmpty()) {
            throw GuardException.forParam(parameterName, "cannot be null or empty");
        }
        return value;
    }

    // --- UUID checks ---

    public static UUID againstEmpty(UUID value, String parameterName) {
        againstNull(value, parameterName);
        if (value.equals(new UUID(0, 0))) {
            throw GuardException.forParam(parameterName, "cannot be an empty UUID");
        }
        return value;
    }

    // --- Condition checks ---

    public static void againstCondition(boolean condition, String parameterName, String message) {
        if (condition) {
            throw GuardException.forParam(parameterName, message);
        }
    }

    public static void ensureCondition(boolean condition, String parameterName, String message) {
        if (!condition) {
            throw GuardException.forParam(parameterName, message);
        }
    }
}
