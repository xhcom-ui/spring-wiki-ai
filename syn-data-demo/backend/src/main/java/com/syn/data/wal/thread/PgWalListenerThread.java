package com.syn.data.wal.thread;

import com.syn.data.wal.config.PgHost;
import com.syn.data.wal.core.PgWalDataDispatcher;
import com.syn.data.wal.core.PgWalListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 启动PostgreSQL WAL监听线程
 * @author user
 */
public class PgWalListenerThread implements Runnable {
    private PgHost host;
    private PgWalDataDispatcher listener;

    private Logger logger = LoggerFactory.getLogger(PgWalListenerThread.class);

    public PgWalListenerThread(PgHost host, PgWalDataDispatcher listener) {
        this.host = host;
        this.listener = listener;
    }

    @Override
    public void run() {
        PgWalListener client = new PgWalListener(
                host.getHost(), 
                host.getPort(), 
                host.getUsername(), 
                host.getPassword(),
                host.getDatabase()
        );
        client.registerEventListener(listener);
        
        for (; ; ) {
            try {
                client.connect();
                break;
            } catch (IOException e) {
                logger.error("PgWalListener启动异常，5秒后尝试重新连接，{}:{}", host.getHost(), host.getPort(), e);
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
