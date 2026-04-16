package com.example.hashmap;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    // Static HashMap that causes memory leak
    private static final Map<Order, OrderDetail> orderCache = new HashMap<>();

    @PostMapping
    public String createOrder(@RequestBody OrderRequest request) {
        Order order = new Order(request.getOrderId(), request.getCustomerId());
        OrderDetail detail = new OrderDetail(order.getOrderId(), request.getAmount(), request.getItems());
        
        // Add to cache
        orderCache.put(order, detail);
        
        System.out.println("Order created. Cache size: " + orderCache.size());
        return "Order created successfully: " + order.getOrderId();
    }

    @GetMapping("/{orderId}")
    public OrderDetail getOrder(@PathVariable String orderId) {
        // Try to retrieve from cache
        Order lookupOrder = new Order(orderId, ""); // Customer ID not provided in lookup
        OrderDetail detail = orderCache.get(lookupOrder);
        
        if (detail != null) {
            System.out.println("Order found in cache");
            return detail;
        } else {
            System.out.println("Order not found in cache");
            // Simulate database lookup
            return new OrderDetail(orderId, 0.0, 0);
        }
    }

    @GetMapping("/cache/size")
    public Map<String, Integer> getCacheSize() {
        return Map.of("cacheSize", orderCache.size());
    }

    // Inner classes to simulate the issue
    static class Order {
        private String orderId;
        private String customerId;

        public Order(String orderId, String customerId) {
            this.orderId = orderId;
            this.customerId = customerId;
        }

        // Missing equals() and hashCode() methods
        // This causes each new Order object to be treated as a different key
        // even if they have the same orderId

        public String getOrderId() {
            return orderId;
        }

        public String getCustomerId() {
            return customerId;
        }
    }

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
