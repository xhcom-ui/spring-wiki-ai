package com.example.hashmap;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/fixed-orders")
public class FixedOrderController {

    // Static HashMap with proper equals() and hashCode() implementation
    private static final Map<FixedOrder, OrderDetail> orderCache = new HashMap<>();

    @PostMapping
    public String createOrder(@RequestBody OrderRequest request) {
        FixedOrder order = new FixedOrder(request.getOrderId(), request.getCustomerId());
        OrderDetail detail = new OrderDetail(order.getOrderId(), request.getAmount(), request.getItems());
        
        // Add to cache
        orderCache.put(order, detail);
        
        System.out.println("Fixed order created. Cache size: " + orderCache.size());
        return "Order created successfully: " + order.getOrderId();
    }

    @GetMapping("/{orderId}")
    public OrderDetail getOrder(@PathVariable String orderId) {
        // Try to retrieve from cache
        FixedOrder lookupOrder = new FixedOrder(orderId, ""); // Customer ID not provided in lookup
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

    // Fixed Order class with proper equals() and hashCode() methods
    static class FixedOrder {
        private String orderId;
        private String customerId;

        public FixedOrder(String orderId, String customerId) {
            this.orderId = orderId;
            this.customerId = customerId;
        }

        // Proper equals() method that only considers orderId
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FixedOrder that = (FixedOrder) o;
            return Objects.equals(orderId, that.orderId);
        }

        // Proper hashCode() method that only uses orderId
        @Override
        public int hashCode() {
            return Objects.hash(orderId);
        }

        public String getOrderId() {
            return orderId;
        }

        public String getCustomerId() {
            return customerId;
        }
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
