package org.wiki.service;

import cn.hutool.core.collection.RingIndexUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleLoadBalancer {
    private final List<String> servers;
    private final AtomicInteger index;

    public SimpleLoadBalancer(List<String> servers) {
        this.servers = new ArrayList<>(servers);
        this.index = new AtomicInteger(0);
    }

    /**
     * 获取下一个服务器地址
     */
    public String nextServer() {
        if (servers.isEmpty()) {
            throw new IllegalStateException("没有可用的服务器");
        }
        int idx = RingIndexUtil.ringNextInt(servers.size(), index);
        return servers.get(idx);
    }

    /**
     * 添加服务器
     */
    public void addServer(String server) {
        servers.add(server);
    }

    /**
     * 移除服务器
     */
    public void removeServer(String server) {
        servers.remove(server);
    }

    /**
     * 获取服务器数量
     */
    public int serverCount() {
        return servers.size();
    }


    public static void main(String[] args) {
        // 创建负载均衡器
        List servers = Arrays.asList(
                "http://server1:8080",
                "http://server2:8080",
                "http://server3:8080"
        );
        SimpleLoadBalancer balancer = new SimpleLoadBalancer(servers);

        // 模拟请求分发
        for (int i = 0; i < 9; i++) {
            String server = balancer.nextServer();
            System.out.println("请求" + (i + 1) + " -> " + server);
        }
       // 每个服务器各处理3次请求，完全均衡
    }
}