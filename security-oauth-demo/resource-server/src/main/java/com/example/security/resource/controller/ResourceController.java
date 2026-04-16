package com.example.security.resource.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ResourceController {

    // 公共接口
    @GetMapping("/public/hello")
    public Map<String, Object> publicHello() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "Hello from public endpoint");
        return result;
    }

    // 需要认证的接口
    @GetMapping("/protected/hello")
    public Map<String, Object> protectedHello(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "Hello from protected endpoint");
        result.put("user", jwt.getSubject());
        result.put("claims", jwt.getClaims());
        return result;
    }

    // 需要 read 权限的接口
    @GetMapping("/read/data")
    public Map<String, Object> readData(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "Read data successfully");
        result.put("user", jwt.getSubject());
        result.put("data", "This is read-only data");
        return result;
    }

    // 需要 write 权限的接口
    @GetMapping("/write/data")
    public Map<String, Object> writeData(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "Write data successfully");
        result.put("user", jwt.getSubject());
        result.put("data", "This is writeable data");
        return result;
    }
}
