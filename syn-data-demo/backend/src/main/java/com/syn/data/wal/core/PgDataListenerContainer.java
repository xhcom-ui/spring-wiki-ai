package com.syn.data.wal.core;

import com.syn.data.wal.listener.IPgDataListener;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL数据监听器容器
 * 用于管理监听器和调用监听器方法
 */
@Slf4j
public class PgDataListenerContainer {
    private Class<?> entityClass;
    private IPgDataListener<?> listener;
    private String[] columns;
    private long timeOffset;

    public PgDataListenerContainer(Class<?> entityClass, IPgDataListener<?> listener, String[] columns, long timeOffset) {
        this.entityClass = entityClass;
        this.listener = listener;
        this.columns = columns;
        this.timeOffset = timeOffset;
    }

    public void invokeUpdate(List<Map.Entry<Serializable[], Serializable[]>> rows) {
        for (Map.Entry<Serializable[], Serializable[]> row : rows) {
            try {
                Object from = createEntity(row.getKey());
                Object to = createEntity(row.getValue());
                listener.onUpdate(from, to);
            } catch (Exception e) {
                log.error("调用监听器onUpdate方法失败", e);
            }
        }
    }

    public void invokeDelete(List<Serializable[]> rows) {
        for (Serializable[] row : rows) {
            try {
                Object data = createEntity(row);
                listener.onDelete(data);
            } catch (Exception e) {
                log.error("调用监听器onDelete方法失败", e);
            }
        }
    }

    public void invokeInsert(List<Serializable[]> rows) {
        for (Serializable[] row : rows) {
            try {
                Object data = createEntity(row);
                listener.onInsert(data);
            } catch (Exception e) {
                log.error("调用监听器onInsert方法失败", e);
            }
        }
    }

    private Object createEntity(Serializable[] values) throws Exception {
        Constructor<?> constructor = entityClass.getDeclaredConstructor();
        Object entity = constructor.newInstance();
        
        // 这里需要根据实际的实体类结构进行属性赋值
        // 目前只是一个简单的实现
        
        return entity;
    }
}
