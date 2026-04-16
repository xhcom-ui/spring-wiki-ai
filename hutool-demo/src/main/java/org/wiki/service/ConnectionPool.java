package org.wiki.service;

import cn.hutool.core.collection.RingIndexUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
//场景2：数据库连接池轮询
public class ConnectionPool {
    private final Connection[] connections;
    private final AtomicInteger index;
    private final int poolSize;

    public ConnectionPool(String url, int poolSize) throws SQLException {
        this.poolSize = poolSize;
        this.connections = new Connection[poolSize];
        this.index = new AtomicInteger(0);

        // 初始化连接
        for (int i = 0; i < poolSize; i++) {
            connections[i] = DriverManager.getConnection(url);
            System.out.println("创建连接" + (i + 1));
        }
    }

    /**
     * 获取连接（轮询方式）
     */
    public Connection getConnection() {
        int idx = RingIndexUtil.ringNextInt(poolSize, index);
        return connections[idx];
    }

    /**
     * 关闭所有连接
     */
    public void close() {
        for (Connection conn : connections) {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                // 忽略关闭异常
            }
        }
    }
}