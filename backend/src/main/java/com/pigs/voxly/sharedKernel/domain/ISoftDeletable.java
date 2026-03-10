package com.pigs.voxly.sharedKernel.domain;

import java.time.Instant;

public interface ISoftDeletable {

    boolean isDeleted();

    Instant getDeletedAt();
}
