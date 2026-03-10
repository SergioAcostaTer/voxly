package com.pigs.voxly.sharedKernel.domain.results;

import java.util.Objects;

public final class Error {

    public static final Error NONE = new Error("", "", ErrorType.NONE);

    private final String code;
    private final String message;
    private final ErrorType type;

    private Error(String code, String message, ErrorType type) {
        this.code = code;
        this.message = message;
        this.type = type;
    }

    public static Error failure(String code, String message) {
        return new Error(code, message, ErrorType.FAILURE);
    }

    public static Error validation(String code, String message) {
        return new Error(code, message, ErrorType.VALIDATION);
    }

    public static Error notFound(String code, String message) {
        return new Error(code, message, ErrorType.NOT_FOUND);
    }

    public static Error conflict(String code, String message) {
        return new Error(code, message, ErrorType.CONFLICT);
    }

    public static Error unauthorized(String code, String message) {
        return new Error(code, message, ErrorType.UNAUTHORIZED);
    }

    public static Error forbidden(String code, String message) {
        return new Error(code, message, ErrorType.FORBIDDEN);
    }

    public static Error unexpected(String code, String message) {
        return new Error(code, message, ErrorType.UNEXPECTED);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public ErrorType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Error error = (Error) o;
        return Objects.equals(code, error.code) && type == error.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, type);
    }

    @Override
    public String toString() {
        return "[%s] %s".formatted(code, message);
    }
}
