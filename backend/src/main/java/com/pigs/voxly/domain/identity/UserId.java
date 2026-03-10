package com.pigs.voxly.domain.identity;

import com.pigs.voxly.sharedKernel.domain.types.StronglyTypedId;

import java.util.UUID;

public final class UserId extends StronglyTypedId.UuidId<UserId> {

    public UserId(UUID value) {
        super(value);
    }

    public static UserId create() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId from(UUID value) {
        return new UserId(value);
    }
}
