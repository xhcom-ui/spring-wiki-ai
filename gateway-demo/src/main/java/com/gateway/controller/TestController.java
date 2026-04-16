package com.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 测试控制器
 */
@RestController
@RequestMapping("/test")
public class TestController {

    /**
     * 测试接口
     */
    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("message", "Hello, Gateway!");
    }

    /**
     * 测试接口2
     */
    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "status", "ok",
                "message", "Gateway is working properly"
        );
    }
}
