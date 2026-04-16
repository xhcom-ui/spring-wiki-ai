package com.syn.data.controller;

import com.syn.data.codegen.ListenerCodeGenerator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 代码生成控制器
 * 用于处理前端的代码生成请求
 */
@RestController
@RequestMapping("/api/codegen")
public class CodeGenController {

    /**
     * 生成PostgreSQL监听器代码
     */
    @PostMapping("/pg-listener")
    public String generatePgListener(@RequestBody Map<String, String> config) {
        return ListenerCodeGenerator.generatePgListenerCode(config);
    }

    /**
     * 生成MySQL监听器代码
     */
    @PostMapping("/mysql-listener")
    public String generateMysqlListener(@RequestBody Map<String, String> config) {
        return ListenerCodeGenerator.generateMysqlListenerCode(config);
    }
}
