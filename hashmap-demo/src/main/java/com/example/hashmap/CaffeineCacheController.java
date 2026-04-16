package com.example.hashmap;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/caffeine-orders")
public class CaffeineCacheController {

    // In-memory store to simulate database
    private final Map<String, OrderDetail> orderStore = new HashMap<>();

    @PostMapping
    @CachePut(value = "orders", key = "#request.orderId")
    public OrderDetail createOrder(@RequestBody OrderRequest request) {
        OrderDetail detail = new OrderDetail(request.getOrderId(), request.getAmount(), request.getItems());
        orderStore.put(request.getOrderId(), detail);
        System.out.println("Created order: " + request.getOrderId());
        return detail;
    }

    @GetMapping("/{orderId}")
    @Cacheable(value = "orders", key = "#orderId")
    public OrderDetail getOrder(@PathVariable String orderId) {
        // This method will only be called if the order is not in the cache
        System.out.println("Fetching order from store: " + orderId);
        OrderDetail detail = orderStore.get(orderId);
        if (detail == null) {
            // Simulate database lookup
            detail = new OrderDetail(orderId, 0.0, 0);
        }
        return detail;
    }

    @PutMapping("/{orderId}")
    @CachePut(value = "orders", key = "#orderId")
    public OrderDetail updateOrder(@PathVariable String orderId, @RequestBody OrderRequest request) {
        OrderDetail detail = new OrderDetail(orderId, request.getAmount(), request.getItems());
        orderStore.put(orderId, detail);
        System.out.println("Updated order: " + orderId);
        return detail;
    }

    @DeleteMapping("/{orderId}")
    @CacheEvict(value = "orders", key = "#orderId")
    public String deleteOrder(@PathVariable String orderId) {
        orderStore.remove(orderId);
        System.out.println("Deleted order: " + orderId);
        return "Order deleted successfully: " + orderId;
    }

    @GetMapping("/cache/clear")
    @CacheEvict(value = "orders", allEntries = true)
    public String clearCache() {
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
