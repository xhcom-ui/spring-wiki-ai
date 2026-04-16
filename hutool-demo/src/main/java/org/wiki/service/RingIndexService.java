package org.wiki.service;

import cn.hutool.core.collection.RingIndexUtil;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RingIndexService {

    public static void main(String[] args) {

        // 创建原子计数器
        AtomicInteger index = new AtomicInteger(0);
        // 环大小为3，索引范围0-2
        int modulo = 3;

        // 连续调用观察回环效果
        System.out.println(RingIndexUtil.ringNextInt(modulo, index));  // 0
        System.out.println(RingIndexUtil.ringNextInt(modulo, index));  // 1
        System.out.println(RingIndexUtil.ringNextInt(modulo, index));  // 2
        System.out.println(RingIndexUtil.ringNextInt(modulo, index));  // 0 (回环)
        System.out.println(RingIndexUtil.ringNextInt(modulo, index));  // 1
        System.out.println(RingIndexUtil.ringNextInt(modulo, index));  // 2
        System.out.println(RingIndexUtil.ringNextInt(modulo, index));  // 0 (再次回环)


        // 服务器列表
        String[] servers = {"192.168.1.1", "192.168.1.2", "192.168.1.3"};
        AtomicInteger serverIndex = new AtomicInteger(0);

        // 模拟10次请求，轮流分配到不同服务器
        for (int i = 0; i < 10; i++) {
            int idx = RingIndexUtil.ringNextInt(servers.length, serverIndex);
            System.out.println("请求" + (i + 1) + " -> " + servers[idx]);
        }


        // 使用long类型模数
        long modulo1 = 5L;
        // 创建原子计数器
        AtomicLong index1 = new AtomicLong(0);

        // 连续调用
        System.out.println(RingIndexUtil.ringNextLong(modulo1, index1));  // 0
        System.out.println(RingIndexUtil.ringNextLong(modulo1, index1));  // 1
        System.out.println(RingIndexUtil.ringNextLong(modulo1, index1));  // 2
        System.out.println(RingIndexUtil.ringNextLong(modulo1, index1));  // 3
        System.out.println(RingIndexUtil.ringNextLong(modulo1, index1));  // 4
        System.out.println(RingIndexUtil.ringNextLong(modulo1, index1));  // 0 (回环)
    }
}
