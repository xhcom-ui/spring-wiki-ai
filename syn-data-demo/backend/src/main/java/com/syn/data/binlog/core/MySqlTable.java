package com.syn.data.binlog.core;

import lombok.Getter;
import lombok.Setter;

/**
 * MySQL表信息
 * 用于存储表的相关信息
 */
@Getter
@Setter
public class MySqlTable {
    private long id;
    private String database;
    private String table;

    public MySqlTable(Object data) {
        // 这里需要根据实际的TableMapEventData格式进行解析
        // 目前只是一个简单的实现
    }
}
