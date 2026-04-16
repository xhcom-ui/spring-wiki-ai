package com.example.skapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class Config {

    @Value("${api.keys}")
    private List<String> apiKeys;

    @Value("${api.rate-limit.capacity}")
    private double bucketCapacity;

    @Value("${api.rate-limit.fill-rate}")
    private double bucketFillRate;

    @Value("${api.retry.max-retries}")
    private int maxRetries;

    @Bean
    public APIRequestManager apiRequestManager() {
        return new APIRequestManager(apiKeys, bucketCapacity, bucketFillRate, maxRetries);
    }
}
