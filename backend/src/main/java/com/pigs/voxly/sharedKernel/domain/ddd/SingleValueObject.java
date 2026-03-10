package com.pigs.voxly.sharedKernel.domain.ddd;

import java.util.List;

public abstract class SingleValueObject<T> extends ValueObject {

    private final T value;

    protected SingleValueObject(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    @Override
    protected List<Object> equalityComponents() {
        return List.of(value);
    }

    @Override
    public String toString() {
        return value != null ? value.toString() : "";
    }
}
