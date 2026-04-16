package com.syn.data.wal.listener;

/**
 * PostgreSQL WAL监听回调业务接口
 * @author user
 * @param <T>
 */
public interface IPgDataListener<T> {
    /**
     * 更新数据
     *
     * @param from
     * @param to
     */
    void onUpdate(T from, T to);

    /**
     * 新增数据
     * @param data
     */
    void onInsert(T data);

    /**
     * 删除数据
     * @param data
     */
    void onDelete(T data);
}
