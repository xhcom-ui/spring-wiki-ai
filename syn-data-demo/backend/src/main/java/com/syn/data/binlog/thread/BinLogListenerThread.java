package com.syn.data.binlog.thread;

import com.syn.data.binlog.config.MySqlHost;
import com.syn.data.binlog.core.BinlogDataDispatcher;
import com.syn.data.binlog.core.MySQLBinlogListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 启动mysql binglog监听线程
 * @author user
 */
public class BinLogListenerThread implements Runnable {
    private MySqlHost host;
    private BinlogDataDispatcher listener;

    private Logger logger = LoggerFactory.getLogger(BinLogListenerThread.class);

    public BinLogListenerThread(MySqlHost host, BinlogDataDispatcher listener) {
        this.host = host;
        this.listener = listener;
    }

    @Override
    public void run() {
        MySQLBinlogListener client = new MySQLBinlogListener(
                host.getHost(), 
                host.getPort(), 
                host.getUsername(), 
                host.getPassword()
        );
        client.setServerId(host.getServerId());
        client.registerEventListener(listener);
        
        for (; ; ) {
            try {
                client.connect();
                break;
            } catch (IOException e) {
                logger.error("MySQLBinlogListener启动异常，5秒后尝试重新连接，{}:{}", host.getHost(), host.getPort(), e);
                try {
                    //如果连接失败5秒后重试
                    Thread.sleep(5000L);
                } catch (InterruptedException ie) {
                    logger.error("InterruptedException ", e);
                }
            }
        }
    }
}
