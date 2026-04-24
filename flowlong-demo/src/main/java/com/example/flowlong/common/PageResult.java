package com.example.flowlong.common;

import lombok.Getter;

import java.util.List;
import java.util.function.Function;

@Getter
public class PageResult<T> {
    private final List<T> items;
    private final long total;
    private final int page;
    private final int size;

    public PageResult(List<T> items, long total, int page, int size) {
        this.items = items == null ? List.of() : items;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(items.stream().map(mapper).toList(), total, page, size);
    }
}
