package com.example.health.datasource;

import com.example.health.metrics.ConnectionMetrics;
import com.example.health.service.ConnectionLeakDetector;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

public class DataSourceProxy implements DataSource {

    private final DataSource delegate;
    private final ConnectionLeakDetector leakDetector;
    private final ConnectionMetrics metrics;

    public DataSourceProxy(DataSource delegate, ConnectionLeakDetector leakDetector, ConnectionMetrics metrics) {
        this.delegate = delegate;
        this.leakDetector = leakDetector;
        this.metrics = metrics;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = delegate.getConnection();
        return new ConnectionProxy(connection, leakDetector, metrics);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = delegate.getConnection(username, password);
        return new ConnectionProxy(connection, leakDetector, metrics);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return delegate.isWrapperFor(iface);
    }
}
