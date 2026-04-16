package com.syn.data.binlog.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MySQL主机配置管理
 * 负责加载和管理MySQL主机配置
 */
@Slf4j
@Component
public class MySqlHostProfile {

    // 从配置文件加载MySQL主机配置
    @Value("${mysql.hosts:}")
    private String hostsConfig;

    // 存储所有MySQL主机配置
    private final Map<String, MySqlHost> hostMap = new HashMap<>();

    /**
     * 初始化时加载MySQL主机配置
     */
    @PostConstruct
    public void init() {
        log.info("初始化MySQL主机配置...");
        loadHosts();
    }

    /**
     * 加载MySQL主机配置
     * 配置格式: host1:port1:username1:password1,host2:port2:username2:password2
     */
    private void loadHosts() {
        if (hostsConfig != null && !hostsConfig.isEmpty()) {
            String[] hosts = hostsConfig.split(",");
            for (int i = 0; i < hosts.length; i++) {
                String hostStr = hosts[i];
                String[] parts = hostStr.split(":");
                if (parts.length >= 4) {
                    MySqlHost host = new MySqlHost();
                    host.setName("mysql-" + (i + 1));
                    host.setServerId(1000 + i);
                    host.setHost(parts[0]);
                    host.setPort(Integer.parseInt(parts[1]));
                    host.setUsername(parts[2]);
                    host.setPassword(parts[3]);
                    host.setTimeOffset(0);
                    hostMap.put(host.getName(), host);
                    log.info("加载MySQL主机配置: {}", host.getName());
                }
            }
        }

        // 这里可以添加从数据库加载配置的逻辑
    }

    /**
     * 根据名称获取MySQL主机配置
     */
    public MySqlHost getByName(String name) {
        return hostMap.get(name);
    }

    /**
     * 根据名称获取MySQL主机配置，如果不存在则抛出异常
     */
    public MySqlHost getByNameAndThrow(String name) { 
        MySqlHost host = getByName(name);
        if (host == null) {
            throw new RuntimeException("MySQL主机配置不存在: " + name);
        }
        return host;
    }

    /**
     * 获取所有MySQL主机配置
     */
    public List<MySqlHost> getAllHosts() {
        return new ArrayList<>(hostMap.values());
    }

    /**
     * 添加MySQL主机配置
     */
    public void addHost(MySqlHost host) {
        hostMap.put(host.getName(), host);
    }

    /**
     * 删除MySQL主机配置
     */
    public void removeHost(String name) {
        hostMap.remove(name);
    }
}
