package com.pigs.voxly.application.sessions.dto;

import com.pigs.voxly.domain.sessions.Session;
import com.pigs.voxly.sharedKernel.domain.pagination.PagedList;

import java.util.List;

public record SessionListResponse(
        List<SessionResponse> sessions,
        int page,
        int size,
        int totalElements,
        int totalPages
) {
    public static SessionListResponse fromPagedList(PagedList<Session> pagedList) {
        return new SessionListResponse(
                pagedList.getItems().stream().map(SessionResponse::fromDomain).toList(),
                pagedList.getPageNumber(),
                pagedList.getPageSize(),
                pagedList.getTotalCount(),
                pagedList.getTotalPages()
        );
    }
}
