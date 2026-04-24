package com.example.flowlong.controller.dto;

import com.example.flowlong.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MenuQueryRequest extends PageQuery {
    private String keyword;
    private Integer status;

    public String normalizedKeyword() {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    public String getNormalizedKeyword() {
        return normalizedKeyword();
    }
}
