package com.pigs.voxly.domain.identity.valueobjects;

import com.pigs.voxly.domain.identity.UserErrors;
import com.pigs.voxly.sharedKernel.domain.ddd.ValueObject;
import com.pigs.voxly.sharedKernel.domain.results.ResultT;

import java.util.List;
import java.util.regex.Pattern;

public final class Email extends ValueObject {

    public static final int MAX_LENGTH = 256;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    private final String value;

    private Email(String value) {
        this.value = value;
    }

    public static ResultT<Email> create(String rawValue) {
        String normalized = rawValue == null ? "" : rawValue.trim().toLowerCase();

        if (normalized.isBlank()) {
            return ResultT.failure(UserErrors.EMAIL_REQUIRED);
        }
        if (normalized.length() > MAX_LENGTH) {
            return ResultT.failure(UserErrors.emailTooLong(MAX_LENGTH));
        }
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            return ResultT.failure(UserErrors.EMAIL_INVALID_FORMAT);
        }

        return ResultT.success(new Email(normalized));
    }

    public static Email reconstitute(String value) {
        return new Email(value);
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
