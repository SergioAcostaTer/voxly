package com.pigs.voxly.domain.sessions;

import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.sharedKernel.domain.pagination.PagedList;
import com.pigs.voxly.sharedKernel.domain.pagination.PagedRequest;

import java.util.Optional;

public interface SessionRepository {

    Optional<Session> findById(SessionId id);

    Optional<Session> findByIdAndUserId(SessionId id, UserId userId);

    PagedList<Session> findByUserId(UserId userId, PagedRequest pageRequest);

    long countByUserId(UserId userId);

    void save(Session session);

    void delete(Session session);
}
