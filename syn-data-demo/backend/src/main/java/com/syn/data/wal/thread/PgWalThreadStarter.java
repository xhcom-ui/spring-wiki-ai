package com.syn.data.wal.thread;

import com.syn.data.wal.config.PgHost;
import com.syn.data.wal.core.PgDataListenerContainer;
import com.syn.data.wal.core.PgDataListenerData;
import com.syn.data.wal.core.PgWalDataDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PostgreSQL WAL监听线程启动器
 * 负责为每个PostgreSQL主机启动WAL监听线程
 */
@Slf4j
public class PgWalThreadStarter {

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            log.error("加载PostgreSQL驱动失败", e);
            throw new RuntimeException(e);
        }
    }

    // 连接池
    private final Map<String, Connection> connectionPool = new HashMap<>();

    /**
     * 获取数据库连接
     */
    private Connection getConnection(PgHost host) throws SQLException {
        String key = host.getHost() + ":" + host.getPort() + ":" + host.getDatabase();
        Connection connection = connectionPool.get(key);
        if (connection == null || connection.isClosed()) {
            String url = "jdbc:postgresql://" + host.getHost() + ":" + host.getPort() + "/" + host.getDatabase() +
                    "?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=UTC";
            connection = DriverManager.getConnection(url, host.getUsername(), host.getPassword());
            connectionPool.put(key, connection);
        }
        return connection;
    }

    /**
     * 释放数据库连接
     */
    private void releaseConnection() {
        for (Map.Entry<String, Connection> entry : connectionPool.entrySet()) {
            try {
                if (!entry.getValue().isClosed()) {
                    entry.getValue().close();
                }
            } catch (SQLException e) {
                log.error("释放数据库连接失败", e);
            }
        }
        connectionPool.clear();
    }

    /**
     * 为指定PostgreSQL主机启动WAL监听线程
     */
    public void runThread(PgHost host, List<PgDataListenerData> listeners, RedisTemplate<String, Object> redisTemplate) {
        try {
            // 按数据库和表分组
            Map<String, List<PgDataListenerData>> map = listeners.stream()
                    .collect(Collectors.groupingBy(l -> l.getDatabase() + ":" + l.getTable()));

            // 创建WAL数据分发器
            PgWalDataDispatcher walListener = new PgWalDataDispatcher();
            walListener.setRedisTemplate(redisTemplate);

            // 为每个表添加监听器
            map.forEach((k, v) -> {
                try {
                    String[] arr = k.split(":");
                    String database = arr[0];
                    String table = arr[1];

                    // 获取表的列信息
                    String[] columns = getColumns(host, database, table);

                    // 创建数据监听器容器
                    List<PgDataListenerContainer> containers = v.stream()
                            .map(l -> new PgDataListenerContainer(l.getEntityClass(), l.getListener(), columns, host.getTimeOffset()))
                            .collect(Collectors.toList());

                    // 添加监听器
                    walListener.addListener(database, table, containers);
                    log.info("为表 {}.{} 添加监听器", database, table);
                } catch (Exception e) {
                    log.error("为表 {} 添加监听器失败", k, e);
                }
            });

            // 启动WAL监听线程
            new Thread(() -> {
                try {
                    PgWalListenerThread listenerThread = new PgWalListenerThread(host, walListener);
                    listenerThread.run();
                } catch (Exception e) {
                    log.error("启动WAL监听线程失败", e);
                }
            }).start();

            // 释放数据库连接
            releaseConnection();
        } catch (Exception e) {
            log.error("启动WAL监听线程失败", e);
            releaseConnection();
        }
    }

    /**
     * 获取表的列信息
     */
    private String[] getColumns(PgHost host, String db, String table) throws SQLException {
        Connection connection = getConnection(host);
        Statement statement = connection.createStatement();

        String sql = "SELECT column_name FROM information_schema.columns WHERE table_schema = 'public' AND table_name = '" + table + "' ORDER BY ordinal_position ASC";

        ResultSet resultSet = statement.executeQuery(sql);
        List<String> columns = new ArrayList<>();

        while (resultSet.next()) {
            columns.add(resultSet.getString(1));
        }

        resultSet.close();
        statement.close();

        return columns.toArray(new String[0]);
    }
}
