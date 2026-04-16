package com.syn.data.binlog.core;

/**
 * MySQL Binlog事件监听器接口
 * 用于处理MySQL Binlog事件
 */
public interface MySQLBinlogEventListener {
    /**
     * 处理Binlog事件
     * @param event Binlog事件
     */
    void onEvent(Object event);
}
