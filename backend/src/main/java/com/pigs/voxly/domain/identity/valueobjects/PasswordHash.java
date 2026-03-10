package com.pigs.voxly.domain.identity.valueobjects;

import com.pigs.voxly.domain.identity.UserErrors;
import com.pigs.voxly.sharedKernel.domain.ddd.ValueObject;
import com.pigs.voxly.sharedKernel.domain.results.ResultT;

import java.util.List;

public final class PasswordHash extends ValueObject {

    private final String value;

    private PasswordHash(String value) {
        this.value = value;
    }

    public static ResultT<PasswordHash> create(String hashedValue) {
        if (hashedValue == null || hashedValue.isBlank()) {
            return ResultT.failure(UserErrors.PASSWORD_HASH_REQUIRED);
        }
        return ResultT.success(new PasswordHash(hashedValue));
    }

    public static PasswordHash reconstitute(String value) {
        return new PasswordHash(value);
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
        return "****";
    }
}
