package org.wiki.service;

import cn.hutool.core.collection.RingIndexUtil;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
//场景3：消息队列分区轮询
public class PartitionedMessageQueue {
    private final int partitionCount;
    private final List<List<String>> partitions;
    private final AtomicInteger producerIndex;
    private final AtomicInteger[] consumerIndexes;

    public PartitionedMessageQueue(int partitionCount) {
        this.partitionCount = partitionCount;
        this.partitions = new ArrayList<>();
        this.producerIndex = new AtomicInteger(0);
        this.consumerIndexes = new AtomicInteger[partitionCount];

        for (int i = 0; i < partitionCount; i++) {
            partitions.add(Collections.synchronizedList(new ArrayList<>()));
            consumerIndexes[i] = new AtomicInteger(0);
        }
    }

    /**
     * 发送消息（轮询分发到各分区）
     */
    public void send(String message) {
        int partition = RingIndexUtil.ringNextInt(partitionCount, producerIndex);
        partitions.get(partition).add(message);
        System.out.println("消息发送到分区" + partition + ": " + message);
    }

    /**
     * 批量发送消息
     */
    public void sendBatch(List<String> messages) {
        for (String msg : messages) {
            send(msg);
        }
    }

    /**
     * 获取指定分区的消息数量
     */
    public int getPartitionSize(int partition) {
        return partitions.get(partition).size();
    }

    /**
     * 获取所有分区的消息分布
     */
    public Map<Integer, Integer> getDistribution() {
        Map<Integer, Integer> dist = new HashMap<>();
        for (int i = 0; i < partitionCount; i++) {
            dist.put(i, partitions.get(i).size());
        }
        return dist;
    }
}