package com.pigs.voxly.sharedKernel.domain.pagination;

public class PagedRequest {

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    private final int pageNumber;
    private final int pageSize;
    private final String sortBy;
    private final boolean sortDescending;

    public PagedRequest(int pageNumber, int pageSize) {
        this(pageNumber, pageSize, null, false);
    }

    public PagedRequest(int pageNumber, int pageSize, String sortBy, boolean sortDescending) {
        this.pageNumber = Math.max(1, pageNumber);
        this.pageSize = Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE));
        this.sortBy = sortBy;
        this.sortDescending = sortDescending;
    }

    public static PagedRequest of(int pageNumber, int pageSize) {
        return new PagedRequest(pageNumber, pageSize);
    }

    public static PagedRequest firstPage() {
        return new PagedRequest(1, DEFAULT_PAGE_SIZE);
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public String getSortBy() {
        return sortBy;
    }

    public boolean isSortDescending() {
        return sortDescending;
    }

    public int getOffset() {
        return (pageNumber - 1) * pageSize;
    }
}
