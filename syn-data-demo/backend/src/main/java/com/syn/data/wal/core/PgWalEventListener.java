package com.syn.data.wal.core;

/**
 * PostgreSQL WAL事件监听器接口
 * 用于处理PostgreSQL WAL事件
 */
public interface PgWalEventListener {
    /**
     * 处理WAL事件
     * @param event WAL事件
     */
    void onEvent(Object event);
}
