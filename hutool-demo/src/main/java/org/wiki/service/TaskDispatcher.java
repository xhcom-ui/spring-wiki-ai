package org.wiki.service;

import cn.hutool.core.collection.RingIndexUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
//场景4：多线程任务分发器
public class TaskDispatcher {
    private final List<BlockingQueue<Runnable>> workerQueues;
    private final List<Thread> workers;
    private final AtomicInteger dispatchIndex;
    private volatile boolean running = true;

    public TaskDispatcher(int workerCount) {
        this.workerQueues = new ArrayList<>();
        this.workers = new ArrayList<>();
        this.dispatchIndex = new AtomicInteger(0);

        // 创建工作线程和队列
        for (int i = 0; i < workerCount; i++) {
            BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
            workerQueues.add(queue);

            final int workerId = i;
            Thread worker = new Thread(() -> {
                while (running || !queue.isEmpty()) {
                    try {
                        Runnable task = queue.poll(100, TimeUnit.MILLISECONDS);
                        if (task != null) {
                            System.out.println("Worker-" + workerId + "执行任务");
                            task.run();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "Worker-" + i);
            workers.add(worker);
            worker.start();
        }
    }

    /**
     * 提交任务（轮询分发）
     */
    public void submit(Runnable task) {
        int idx = RingIndexUtil.ringNextInt(workerQueues.size(), dispatchIndex);
        workerQueues.get(idx).offer(task);
    }

    /**
     * 获取各工作线程的待处理任务数
     */
    public Map<Integer, Integer> getQueueSizes() {
        Map<Integer, Integer> sizes = new HashMap<>();
        for (int i = 0; i < workerQueues.size(); i++) {
            sizes.put(i, workerQueues.get(i).size());
        }
        return sizes;
    }

    /**
     * 关闭分发器
     */
    public void shutdown() {
        running = false;
        for (Thread worker : workers) {
            try {
                worker.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // 创建3个工作线程的分发器
        TaskDispatcher dispatcher = new TaskDispatcher(3);
       // 提交15个任务
        for (int i = 1; i <= 15; i++) {
            final int taskId = i;
            dispatcher.submit(() -> {
                System.out.println("执行任务" + taskId);
                try {
                    Thread.sleep(100);  // 模拟任务执行
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // 等待任务执行
        Thread.sleep(3000);
        dispatcher.shutdown();
        // 每个Worker各执行5个任务
    }
}