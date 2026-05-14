package com.pigs.voxly.domain.evaluation;

import com.pigs.voxly.sharedKernel.domain.types.StronglyTypedId;

import java.util.UUID;

public final class EvaluationId extends StronglyTypedId.UuidId<EvaluationId> {

    private EvaluationId(UUID value) {
        super(value);
    }

    public static EvaluationId create() {
        return new EvaluationId(UUID.randomUUID());
    }

    public static EvaluationId from(UUID value) {
        return new EvaluationId(value);
    }

    public static EvaluationId from(String value) {
        return new EvaluationId(UUID.fromString(value));
    }
}
