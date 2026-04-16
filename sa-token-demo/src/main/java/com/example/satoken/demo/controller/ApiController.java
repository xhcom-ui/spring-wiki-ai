package com.example.satoken.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class ApiController {

    // 测试接口
    @GetMapping("/test")
    public Map<String, Object> test(HttpServletRequest request) {
        // 从请求属性中获取 appId
        Object appId = request.getAttribute("appId");
        log.info("API Key 验证通过，appId: {}", appId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "接口调用成功");
        Map<String, Object> data = new HashMap<>();
        data.put("appId", appId);
        data.put("message", "Hello, API Key!");
        result.put("data", data);
        return result;
    }

    // 另一个测试接口
    @GetMapping("/hello")
    public Map<String, Object> hello(HttpServletRequest request) {
        // 从请求属性中获取 appId
        Object appId = request.getAttribute("appId");
        log.info("API Key 验证通过，appId: {}", appId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "Hello 接口调用成功");
        Map<String, Object> data = new HashMap<>();
        data.put("appId", appId);
        data.put("greeting", "Hello from API!");
        result.put("data", data);
        return result;
    }
}
