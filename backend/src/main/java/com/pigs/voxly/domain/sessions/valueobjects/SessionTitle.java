package com.pigs.voxly.domain.sessions.valueobjects;

import com.pigs.voxly.sharedKernel.domain.ddd.ValueObject;
import com.pigs.voxly.sharedKernel.domain.results.Error;
import com.pigs.voxly.sharedKernel.domain.results.ResultT;

import java.util.List;

public final class SessionTitle extends ValueObject {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 200;

    private final String value;

    private SessionTitle(String value) {
        this.value = value;
    }

    public static ResultT<SessionTitle> create(String value) {
        if (value == null || value.isBlank()) {
            return ResultT.failure(Error.validation("SessionTitle.Required", "Session title is required"));
        }

        String trimmed = value.trim();

        if (trimmed.length() < MIN_LENGTH) {
            return ResultT.failure(Error.validation("SessionTitle.TooShort",
                    "Session title must be at least " + MIN_LENGTH + " character"));
        }

        if (trimmed.length() > MAX_LENGTH) {
            return ResultT.failure(Error.validation("SessionTitle.TooLong",
                    "Session title cannot exceed " + MAX_LENGTH + " characters"));
        }

        return ResultT.success(new SessionTitle(trimmed));
    }

    public static SessionTitle reconstitute(String value) {
        return new SessionTitle(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    protected List<Object> equalityComponents() {
        return List.of(value);
    }
}
