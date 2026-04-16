package com.example.hashmap;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/direct-caffeine-orders")
public class DirectCaffeineCacheController {

    // Direct Caffeine cache instance
    private final Cache<String, OrderDetail> orderCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .recordStats()
            .build();

    // In-memory store to simulate database
    private final Map<String, OrderDetail> orderStore = new HashMap<>();

    @PostMapping
    public OrderDetail createOrder(@RequestBody OrderRequest request) {
        OrderDetail detail = new OrderDetail(request.getOrderId(), request.getAmount(), request.getItems());
        orderStore.put(request.getOrderId(), detail);
        // Put in cache
        orderCache.put(request.getOrderId(), detail);
        System.out.println("Created order: " + request.getOrderId());
        return detail;
    }

    @GetMapping("/{orderId}")
    public OrderDetail getOrder(@PathVariable String orderId) {
        // Try to get from cache first
        OrderDetail detail = orderCache.getIfPresent(orderId);
        if (detail != null) {
            System.out.println("Order found in cache: " + orderId);
            return detail;
        }
        
        // If not in cache, get from store
        System.out.println("Fetching order from store: " + orderId);
        detail = orderStore.get(orderId);
        if (detail == null) {
            // Simulate database lookup
            detail = new OrderDetail(orderId, 0.0, 0);
        }
        // Put in cache
        orderCache.put(orderId, detail);
        return detail;
    }

    @PutMapping("/{orderId}")
    public OrderDetail updateOrder(@PathVariable String orderId, @RequestBody OrderRequest request) {
        OrderDetail detail = new OrderDetail(orderId, request.getAmount(), request.getItems());
        orderStore.put(orderId, detail);
        // Update cache
        orderCache.put(orderId, detail);
        System.out.println("Updated order: " + orderId);
        return detail;
    }

    @DeleteMapping("/{orderId}")
    public String deleteOrder(@PathVariable String orderId) {
        orderStore.remove(orderId);
        // Remove from cache
        orderCache.invalidate(orderId);
        System.out.println("Deleted order: " + orderId);
        return "Order deleted successfully: " + orderId;
    }

    @GetMapping("/cache/stats")
    public Map<String, Object> getCacheStats() {
        com.github.benmanes.caffeine.cache.Stats stats = orderCache.stats();
        Map<String, Object> statsMap = new HashMap<>();
        statsMap.put("hitCount", stats.hitCount());
        statsMap.put("missCount", stats.missCount());
        statsMap.put("hitRate", stats.hitRate());
        statsMap.put("evictionCount", stats.evictionCount());
        statsMap.put("averageLoadPenalty", stats.averageLoadPenalty());
        return statsMap;
    }

    @GetMapping("/cache/clear")
    public String clearCache() {
        orderCache.invalidateAll();
        System.out.println("Cleared all orders from cache");
        return "Cache cleared successfully";
    }

    // Reusing the same OrderDetail class
    static class OrderDetail {
        private String orderId;
        private double amount;
        private int items;

        public OrderDetail(String orderId, double amount, int items) {
            this.orderId = orderId;
            this.amount = amount;
            this.items = items;
        }

        public String getOrderId() {
            return orderId;
        }

        public double getAmount() {
            return amount;
        }

        public int getItems() {
            return items;
        }
    }

    // Reusing the same OrderRequest class
    static class OrderRequest {
        private String orderId;
        private String customerId;
        private double amount;
        private int items;

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        public int getItems() {
            return items;
        }

        public void setItems(int items) {
            this.items = items;
        }
    }
}
