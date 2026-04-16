package com.syn.data.binlog.core;

import com.syn.data.binlog.annotation.MysqlWatcher;
import com.syn.data.binlog.listener.IMysqlDataListener;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * MySQL数据监听器数据
 * 用于存储监听器的相关信息
 */
@Slf4j
@Getter
@Setter
public class MysqlDataListenerData {
    private String hostName;
    private String database;
    private String table;
    private IMysqlDataListener listener;
    private Class entityClass;

    public MysqlDataListenerData(IMysqlDataListener listener) {
        this.listener = listener;
        MysqlWatcher watcher = listener.getClass().getAnnotation(MysqlWatcher.class);
        if (watcher != null) {
            this.hostName = watcher.hostName();
            this.database = watcher.database();
            this.table = watcher.table();
        }

        // 获取泛型类型
        Type genericSuperclass = listener.getClass().getGenericInterfaces()[0];
        if (genericSuperclass instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) genericSuperclass;
            this.entityClass = (Class) type.getActualTypeArguments()[0];
        }
    }
}
