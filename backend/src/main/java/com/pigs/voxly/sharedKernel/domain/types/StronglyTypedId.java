package com.pigs.voxly.sharedKernel.domain.types;

import java.util.Objects;
import java.util.UUID;

public abstract class StronglyTypedId<T extends StronglyTypedId<T, V>, V> {

    private final V value;

    protected StronglyTypedId(V value) {
        this.value = Objects.requireNonNull(value, "ID value cannot be null");
    }

    public V getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StronglyTypedId<?, ?> that = (StronglyTypedId<?, ?>) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }

    public abstract static class UuidId<T extends UuidId<T>> extends StronglyTypedId<T, UUID> {

        protected UuidId(UUID value) {
            super(value);
        }

        public boolean isEmpty() {
            return getValue().equals(new UUID(0, 0));
        }
    }

    public abstract static class LongId<T extends LongId<T>> extends StronglyTypedId<T, Long> {

        protected LongId(Long value) {
            super(value);
        }
    }
}
