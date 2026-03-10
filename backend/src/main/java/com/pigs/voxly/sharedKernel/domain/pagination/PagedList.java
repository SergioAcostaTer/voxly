package com.pigs.voxly.sharedKernel.domain.pagination;

import java.util.List;
import java.util.function.Function;

public final class PagedList<T> {

    private final List<T> items;
    private final int pageNumber;
    private final int pageSize;
    private final int totalCount;

    private PagedList(List<T> items, int pageNumber, int pageSize, int totalCount) {
        this.items = List.copyOf(items);
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
    }

    public static <T> PagedList<T> of(List<T> items, int pageNumber, int pageSize, int totalCount) {
        return new PagedList<>(items, pageNumber, pageSize, totalCount);
    }

    public static <T> PagedList<T> empty(int pageSize) {
        return new PagedList<>(List.of(), 1, pageSize, 0);
    }

    public static <T> PagedList<T> empty() {
        return empty(PagedRequest.DEFAULT_PAGE_SIZE);
    }

    public List<T> getItems() {
        return items;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getTotalPages() {
        return (int) Math.ceil((double) totalCount / pageSize);
    }

    public boolean hasPreviousPage() {
        return pageNumber > 1;
    }

    public boolean hasNextPage() {
        return pageNumber < getTotalPages();
    }

    public <R> PagedList<R> map(Function<T, R> mapper) {
        List<R> mappedItems = items.stream().map(mapper).toList();
        return new PagedList<>(mappedItems, pageNumber, pageSize, totalCount);
    }
}
