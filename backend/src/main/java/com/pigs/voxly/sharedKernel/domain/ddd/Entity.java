package com.pigs.voxly.sharedKernel.domain.ddd;

import java.util.Objects;

public abstract class Entity<ID> {

    protected ID id;

    protected Entity() {
    }

    protected Entity(ID id) {
        this.id = id;
    }

    public ID getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity<?> other = (Entity<?>) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
