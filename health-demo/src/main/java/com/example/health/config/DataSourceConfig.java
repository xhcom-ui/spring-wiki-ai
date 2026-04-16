package com.example.health.config;

import com.example.health.datasource.DataSourceProxy;
import com.example.health.metrics.ConnectionMetrics;
import com.example.health.service.ConnectionLeakDetector;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    public ConnectionLeakDetector connectionLeakDetector() {
        return new ConnectionLeakDetector();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource(ConnectionLeakDetector connectionLeakDetector, ConnectionMetrics connectionMetrics) {
        HikariDataSource hikariDataSource = new HikariDataSource();
        return new DataSourceProxy(hikariDataSource, connectionLeakDetector, connectionMetrics);
    }
}
