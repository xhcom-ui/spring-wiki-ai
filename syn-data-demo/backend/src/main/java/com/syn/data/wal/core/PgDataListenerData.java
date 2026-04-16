package com.syn.data.wal.core;

import com.syn.data.wal.annotation.PgWatcher;
import com.syn.data.wal.listener.IPgDataListener;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * PostgreSQL数据监听器数据
 * 用于存储监听器的相关信息
 */
@Slf4j
@Getter
@Setter
public class PgDataListenerData {
    private String hostName;
    private String database;
    private String table;
    private IPgDataListener listener;
    private Class entityClass;

    public PgDataListenerData(IPgDataListener listener) {
        this.listener = listener;
        PgWatcher watcher = listener.getClass().getAnnotation(PgWatcher.class);
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
