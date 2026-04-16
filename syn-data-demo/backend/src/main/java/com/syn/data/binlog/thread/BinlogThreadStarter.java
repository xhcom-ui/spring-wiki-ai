package com.syn.data.binlog.thread;

import com.syn.data.binlog.config.MySqlHost;
import com.syn.data.binlog.core.BinlogDataDispatcher;
import com.syn.data.binlog.core.DataListenerContainer;
import com.syn.data.binlog.core.MysqlDataListenerData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MySQL Binlog监听线程启动器
 * 负责为每个MySQL主机启动Binlog监听线程
 */
@Slf4j
public class BinlogThreadStarter {

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            log.error("加载MySQL驱动失败", e);
            throw new RuntimeException(e);
        }
    }

    // 连接池
    private final Map<String, Connection> connectionPool = new HashMap<>();

    /**
     * 获取数据库连接
     */
    private Connection getConnection(MySqlHost host) throws SQLException {
        String key = host.getHost() + ":" + host.getPort();
        Connection connection = connectionPool.get(key);
        if (connection == null || connection.isClosed()) {
            String url = "jdbc:mysql://" + key +
                    "/INFORMATION_SCHEMA?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=UTC";
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
     * 为指定MySQL主机启动Binlog监听线程
     */
    public void runThread(MySqlHost host, List<MysqlDataListenerData> listeners, RedisTemplate<String, Object> redisTemplate) {
        try {
            // 按数据库和表分组
            Map<String, List<MysqlDataListenerData>> map = listeners.stream()
                    .collect(Collectors.groupingBy(l -> l.getDatabase() + ":" + l.getTable()));

            // 创建Binlog数据分发器
            BinlogDataDispatcher logListener = new BinlogDataDispatcher();
            logListener.setRedisTemplate(redisTemplate);

            // 为每个表添加监听器
            map.forEach((k, v) -> {
                try {
                    String[] arr = k.split(":");
                    String database = arr[0];
                    String table = arr[1];

                    // 获取表的列信息
                    String[] columns = getColumns(host, database, table);

                    // 创建数据监听器容器
                    List<DataListenerContainer> containers = v.stream()
                            .map(l -> new DataListenerContainer(l.getEntityClass(), l.getListener(), columns, host.getTimeOffset()))
                            .collect(Collectors.toList());

                    // 添加监听器
                    logListener.addListener(database, table, containers);
                    log.info("为表 {}.{} 添加监听器", database, table);
                } catch (Exception e) {
                    log.error("为表 {} 添加监听器失败", k, e);
                }
            });

            // 启动Binlog监听线程
            new Thread(() -> {
                try {
                    BinLogListenerThread listenerThread = new BinLogListenerThread(host, logListener);
                    listenerThread.run();
                } catch (Exception e) {
                    log.error("启动Binlog监听线程失败", e);
                }
            }).start();

            // 释放数据库连接
            releaseConnection();
        } catch (Exception e) {
            log.error("启动Binlog监听线程失败", e);
            releaseConnection();
        }
    }

    /**
     * 获取表的列信息
     */
    private String[] getColumns(MySqlHost host, String db, String table) throws SQLException {
        Connection connection = getConnection(host);
        Statement statement = connection.createStatement();

        String sql = "select COLUMN_NAME from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA='"
                + db + "' and TABLE_NAME='" + table + "' order by ORDINAL_POSITION asc;";

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
