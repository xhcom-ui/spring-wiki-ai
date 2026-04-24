package com.example.flowlong.common;

import java.util.List;

public final class PageBuilder {

    private PageBuilder() {
    }

    public static <T> PageResult<T> build(PageQuery query, List<T> items, long total) {
        return new PageResult<>(items, total, query.safePage(), query.safeSize());
    }
}
