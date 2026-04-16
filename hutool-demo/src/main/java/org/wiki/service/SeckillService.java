package org.wiki.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class SeckillService {

    // 商品库存
    private AtomicInteger stock = new AtomicInteger(100);

    // 成功抢购计数
    private AtomicInteger successCount = new AtomicInteger(0);

    // 失败计数
    private AtomicInteger failCount = new AtomicInteger(0);

    // 锁（用于悲观锁方案）
    private ReentrantLock lock = new ReentrantLock();

    /**
     * 秒杀方法（乐观锁方案 - 使用 AtomicInteger）
     */
    public boolean seckillOptimistic(Long userId, Long productId) {
        // 检查库存
        int currentStock = stock.get();
        if (currentStock <= 0) {
            failCount.incrementAndGet();
            log.debug("库存不足，用户 {} 抢购失败", userId);
            return false;
        }

        // CAS 扣减库存
        while (currentStock > 0) {
            if (stock.compareAndSet(currentStock, currentStock - 1)) {
                successCount.incrementAndGet();
                log.debug("用户 {} 抢购成功，剩余库存：{}", userId, currentStock - 1);
                return true;
            }
            currentStock = stock.get();
        }

        failCount.incrementAndGet();
        log.debug("库存不足，用户 {} 抢购失败", userId);
        return false;
    }

    /**
     * 秒杀方法（悲观锁方案 - 使用 ReentrantLock）
     */
    public boolean seckillPessimistic(Long userId, Long productId) {
        lock.lock();
        try {
            // 检查库存
            int currentStock = stock.get();
            if (currentStock <= 0) {
                failCount.incrementAndGet();
                log.debug("库存不足，用户 {} 抢购失败", userId);
                return false;
            }

            // 扣减库存
            stock.decrementAndGet();
            successCount.incrementAndGet();
            log.debug("用户 {} 抢购成功，剩余库存：{}", userId, currentStock - 1);
            return true;

        } finally {
            lock.unlock();
        }
    }

    /**
     * 重置数据
     */
    public void reset(int initialStock) {
        stock.set(initialStock);
        successCount.set(0);
        failCount.set(0);
    }

    /**
     * 获取统计信息
     */
    public SeckillStats getStats() {
        SeckillStats stats = new SeckillStats();
        stats.setRemainingStock(stock.get());
        stats.setSuccessCount(successCount.get());
        stats.setFailCount(failCount.get());
        return stats;
    }
}

// 统计信息实体
@Data
class SeckillStats {
    private Integer remainingStock;
    private Integer successCount;
    private Integer failCount;
}