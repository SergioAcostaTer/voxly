package com.pigs.voxly.sharedKernel.domain.ddd;

import java.util.List;
import java.util.Objects;

public abstract class ValueObject {

    protected abstract List<Object> equalityComponents();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ValueObject other = (ValueObject) obj;
        return Objects.equals(equalityComponents(), other.equalityComponents());
    }

    @Override
    public int hashCode() {
        return Objects.hash(equalityComponents().toArray());
    }
}
