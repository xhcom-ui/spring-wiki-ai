package com.vd.canary.obmp.binlog.core;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import lombok.extern.slf4j.Slf4j;

/**
 * BinaryLogClient生命周期监听实现
 *
 */
@Slf4j
public class MyLifecycleListener implements BinaryLogClient.LifecycleListener {
    @Override
    public void onConnect(BinaryLogClient client) {
        //log.info("binlog监听BinaryLogClient启动成功,serverId:{},", client.getServerId());
    }

    @Override
    public void onCommunicationFailure(BinaryLogClient client, Exception ex) {
        //log.error("binlog监听onCommunicationFailure,serverId:{},", client.getServerId(), ex);
    }

    @Override
    public void onEventDeserializationFailure(BinaryLogClient client, Exception ex) {
        log.error("binlog监听反序列化event数据异常 ",ex);
    }

    @Override
    public void onDisconnect(BinaryLogClient client) {
        //log.error("binlog监听BinaryLogClient断开连接;serverId:{},", client.getServerId());
    }
}

