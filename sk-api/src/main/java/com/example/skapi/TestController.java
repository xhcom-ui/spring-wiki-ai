package com.example.skapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private APIRequestManager apiRequestManager;

    @GetMapping("/api")
    public String testAPI() {
        boolean success = apiRequestManager.executeAPIRequest(() -> {
            // 模拟API调用
            System.out.println("执行API调用");
            // 随机失败，测试重试机制
            if (Math.random() > 0.7) {
                throw new RuntimeException("API调用失败");
            }
        });

        if (success) {
            return "API调用成功";
        } else {
            return "API调用失败";
        }
    }
}
