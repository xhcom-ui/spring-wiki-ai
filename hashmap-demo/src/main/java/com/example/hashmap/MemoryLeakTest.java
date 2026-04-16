package com.example.hashmap;

import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class MemoryLeakTest {
    public static void main(String[] args) {
        RestTemplate restTemplate = new RestTemplate();
        String baseUrl = "http://localhost:8080/api/orders";

        // Create orders with the same orderId but different customerId
        // This will cause memory leak because Order objects are not properly compared
        for (int i = 0; i < 1000000; i++) {
            Map<String, Object> orderRequest = new HashMap<>();
            orderRequest.put("orderId", "ORDER-001"); // Same orderId
            orderRequest.put("customerId", "CUSTOMER-" + i); // Different customerId
            orderRequest.put("amount", 100.0 + i);
            orderRequest.put("items", 1 + (i % 5));

            try {
                String response = restTemplate.postForObject(baseUrl, orderRequest, String.class);
                if (i % 10000 == 0) {
                    System.out.println("Created " + i + " orders. Response: " + response);
                    // Check cache size
                    Map<String, Integer> cacheSize = restTemplate.getForObject(baseUrl + "/cache/size", Map.class);
                    System.out.println("Current cache size: " + cacheSize.get("cacheSize"));
                }
            } catch (Exception e) {
                System.err.println("Error creating order: " + e.getMessage());
                break;
            }
        }

        System.out.println("Test completed.");
    }
}
