package com.vd.canary.obmp.binlog.listener;

/**
 * mysql binlog监听回调业务接口
 * @author user
 * @param <T>
 */
public interface IMysqlDataListener<T> {
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
