package com.vd.canary.obmp.binlog.listener;

/**
 *  mysql binlog监听回调业务抽象类
 * @param <T>
 */
public abstract class AbstractMysqlDataListener<T> implements IMysqlDataListener<T> {
    @Override
    public void onInsert(T data) {
        onData(data);
    }

    @Override
    public void onUpdate(T from, T to) {
        onData(to);
    }

    @Override
    public void onDelete(T data) {
        onData(data);
    }

    protected abstract void onData(T data);
}
