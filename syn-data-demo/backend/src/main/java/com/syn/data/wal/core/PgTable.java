package com.syn.data.wal.core;

import lombok.Getter;
import lombok.Setter;

/**
 * PostgreSQL表信息
 * 用于存储表的相关信息
 */
@Getter
@Setter
public class PgTable {
    private long id;
    private String database;
    private String table;

    public PgTable(Object data) {
        // 这里需要根据实际的TableMapEventData格式进行解析
        // 目前只是一个简单的实现
    }
}
