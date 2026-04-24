package com.example.flowable.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PageResponseUtils {

    private PageResponseUtils() {
    }

    public static <T> Map<String, Object> paginate(List<T> records, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, size);
        int fromIndex = Math.min(records.size(), (safePage - 1) * safeSize);
        int toIndex = Math.min(records.size(), fromIndex + safeSize);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", records.subList(fromIndex, toIndex));
        result.put("total", records.size());
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }
}
