package com.pigs.voxly.infrastructure.sessions.persistence.repository;

import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.domain.sessions.Session;
import com.pigs.voxly.domain.sessions.SessionId;
import com.pigs.voxly.domain.sessions.SessionRepository;
import com.pigs.voxly.infrastructure.sessions.persistence.mapper.SessionMapper;
import com.pigs.voxly.sharedKernel.domain.pagination.PagedList;
import com.pigs.voxly.sharedKernel.domain.pagination.PagedRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaSessionRepository implements SessionRepository {

    private final SpringDataSessionRepository springDataRepository;
    private final SessionMapper mapper;

    public JpaSessionRepository(SpringDataSessionRepository springDataRepository, SessionMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Session> findById(SessionId id) {
        return springDataRepository.findById(id.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Session> findByIdAndUserId(SessionId id, UserId userId) {
        return springDataRepository.findByIdAndUserId(id.getValue(), userId.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public PagedList<Session> findByUserId(UserId userId, PagedRequest pagedRequest) {
        var pageable = PageRequest.of(
                pagedRequest.getPageNumber() - 1, // Spring Data uses 0-based pages
                pagedRequest.getPageSize()
        );

        var page = springDataRepository.findByUserIdOrderByCreatedAtDesc(userId.getValue(), pageable);

        var sessions = page.getContent().stream()
                .map(mapper::toDomain)
                .toList();

        return PagedList.of(
                sessions,
                pagedRequest.getPageNumber(),
                pagedRequest.getPageSize(),
                (int) page.getTotalElements()
        );
    }

    @Override
    public long countByUserId(UserId userId) {
        return springDataRepository.countByUserId(userId.getValue());
    }

    @Override
    public void save(Session session) {
        springDataRepository.save(mapper.toEntity(session));
    }

    @Override
    public void delete(Session session) {
        springDataRepository.deleteById(session.getId().getValue());
    }
}
