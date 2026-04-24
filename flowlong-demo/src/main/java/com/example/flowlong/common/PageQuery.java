package com.example.flowlong.common;

import lombok.Data;

@Data
public class PageQuery {
    private Integer page = 1;
    private Integer size = 10;

    public int safePage() {
        return page == null || page < 1 ? 1 : page;
    }

    public int getSafePage() {
        return safePage();
    }

    public int safeSize() {
        return size == null || size < 1 ? 10 : size;
    }

    public int getSafeSize() {
        return safeSize();
    }

    public int offset() {
        return (safePage() - 1) * safeSize();
    }

    public int getOffset() {
        return offset();
    }
}
