package com.pigs.voxly.domain.identity.valueobjects;

import com.pigs.voxly.domain.identity.UserErrors;
import com.pigs.voxly.sharedKernel.domain.ddd.ValueObject;
import com.pigs.voxly.sharedKernel.domain.results.ResultT;

import java.util.List;
import java.util.regex.Pattern;

public final class Username extends ValueObject {

    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 50;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");

    private final String value;

    private Username(String value) {
        this.value = value;
    }

    public static ResultT<Username> create(String rawValue) {
        String normalized = rawValue == null ? "" : rawValue.trim().toLowerCase();

        if (normalized.isBlank()) {
            return ResultT.failure(UserErrors.USERNAME_REQUIRED);
        }
        if (normalized.length() < MIN_LENGTH) {
            return ResultT.failure(UserErrors.usernameTooShort(MIN_LENGTH));
        }
        if (normalized.length() > MAX_LENGTH) {
            return ResultT.failure(UserErrors.usernameTooLong(MAX_LENGTH));
        }
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            return ResultT.failure(UserErrors.USERNAME_INVALID_FORMAT);
        }

        return ResultT.success(new Username(normalized));
    }

    public static Username reconstitute(String value) {
        return new Username(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    protected List<Object> equalityComponents() {
        return List.of(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
