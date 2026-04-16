package com.example.redis.service.impl;

import com.example.redis.entity.Product;
import com.example.redis.repository.ProductRepository;
import com.example.redis.service.ProductService;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ProductServiceImpl implements ProductService {

    @Resource
    private ProductRepository productRepository;

    @Autowired
    private RedissonClient redissonClient;

    @Value("${cache.delay.time:500}")
    private long delayTime;

    private static final String CACHE_NAME = "product";
    private static final String DELAY_QUEUE_NAME = "cache:delay:queue";

    @Override
    public Product save(Product product) {
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        Product savedProduct = productRepository.save(product);
        // 双写策略：先更新数据库，再删除缓存
        deleteCache(savedProduct.getId());
        return savedProduct;
    }

    @Cacheable(value = CACHE_NAME, key = "#id")
    @Override
    public Product findById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    @Cacheable(value = CACHE_NAME, key = "'all'")
    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @CacheEvict(value = CACHE_NAME, key = "#id")
    @Override
    public void deleteById(Long id) {
        productRepository.deleteById(id);
        // 同时删除所有商品的缓存
        deleteAllCache();
    }

    @Override
    public Product update(Product product) {
        // 延迟双删策略：先删除缓存，再更新数据库，最后延迟删除缓存
        deleteCache(product.getId());
        
        product.setUpdateTime(LocalDateTime.now());
        Product updatedProduct = productRepository.save(product);
        
        // 延迟删除缓存，解决并发更新问题
        delayDeleteCache(updatedProduct.getId());
        
        // 同时删除所有商品的缓存
        deleteAllCache();
        delayDeleteAllCache();
        
        return updatedProduct;
    }

    /**
     * 删除指定商品的缓存
     */
    private void deleteCache(Long id) {
        RQueue<String> queue = redissonClient.getQueue(DELAY_QUEUE_NAME);
        RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(queue);
        delayedQueue.offer("product:" + id, delayTime, TimeUnit.MILLISECONDS);
    }

    /**
     * 删除所有商品的缓存
     */
    private void deleteAllCache() {
        RQueue<String> queue = redissonClient.getQueue(DELAY_QUEUE_NAME);
        RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(queue);
        delayedQueue.offer("product:all", delayTime, TimeUnit.MILLISECONDS);
    }

    /**
     * 延迟删除指定商品的缓存
     */
    private void delayDeleteCache(Long id) {
        RQueue<String> queue = redissonClient.getQueue(DELAY_QUEUE_NAME);
        RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(queue);
        delayedQueue.offer("product:" + id, delayTime, TimeUnit.MILLISECONDS);
    }

    /**
     * 延迟删除所有商品的缓存
     */
    private void delayDeleteAllCache() {
        RQueue<String> queue = redissonClient.getQueue(DELAY_QUEUE_NAME);
        RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(queue);
        delayedQueue.offer("product:all", delayTime, TimeUnit.MILLISECONDS);
    }
}
