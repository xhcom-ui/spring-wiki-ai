package com.syn.data.wal.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL主机配置管理
 * 负责加载和管理PostgreSQL主机配置
 */
@Slf4j
@Component
public class PgHostProfile {

    // 从配置文件加载PostgreSQL主机配置
    @Value("${postgres.hosts:}")
    private String hostsConfig;

    // 存储所有PostgreSQL主机配置
    private final Map<String, PgHost> hostMap = new HashMap<>();

    /**
     * 初始化时加载PostgreSQL主机配置
     */
    @PostConstruct
    public void init() {
        log.info("初始化PostgreSQL主机配置...");
        loadHosts();
    }

    /**
     * 加载PostgreSQL主机配置
     * 配置格式: host1:port1:username1:password1:database1,host2:port2:username2:password2:database2
     */
    private void loadHosts() {
        if (hostsConfig != null && !hostsConfig.isEmpty()) {
            String[] hosts = hostsConfig.split(",");
            for (int i = 0; i < hosts.length; i++) {
                String hostStr = hosts[i];
                String[] parts = hostStr.split(":");
                if (parts.length >= 5) {
                    PgHost host = new PgHost();
                    host.setName("postgres-" + (i + 1));
                    host.setHost(parts[0]);
                    host.setPort(Integer.parseInt(parts[1]));
                    host.setUsername(parts[2]);
                    host.setPassword(parts[3]);
                    host.setDatabase(parts[4]);
                    host.setTimeOffset(0);
                    hostMap.put(host.getName(), host);
                    log.info("加载PostgreSQL主机配置: {}", host.getName());
                }
            }
        }

        // 这里可以添加从数据库加载配置的逻辑
    }

    /**
     * 根据名称获取PostgreSQL主机配置
     */
    public PgHost getByName(String name) {
        return hostMap.get(name);
    }

    /**
     * 根据名称获取PostgreSQL主机配置，如果不存在则抛出异常
     */
    public PgHost getByNameAndThrow(String name) { 
        PgHost host = getByName(name);
        if (host == null) {
            throw new RuntimeException("PostgreSQL主机配置不存在: " + name);
        }
        return host;
    }

    /**
     * 获取所有PostgreSQL主机配置
     */
    public List<PgHost> getAllHosts() {
        return new ArrayList<>(hostMap.values());
    }

    /**
     * 添加PostgreSQL主机配置
     */
    public void addHost(PgHost host) {
        hostMap.put(host.getName(), host);
    }

    /**
     * 删除PostgreSQL主机配置
     */
    public void removeHost(String name) {
        hostMap.remove(name);
    }
}
