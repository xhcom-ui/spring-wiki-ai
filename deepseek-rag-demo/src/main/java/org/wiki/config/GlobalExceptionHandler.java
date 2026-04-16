package org.wiki.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("全局异常: ", e);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", e.getMessage());

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (e instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST;
        } else if (e.getMessage() != null && e.getMessage().contains("Unauthorized")) {
            status = HttpStatus.UNAUTHORIZED;
        }

        return ResponseEntity.status(status).body(result);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, Object>> handleIOException(IOException e) {
        log.error("IO异常（文档上传/解析失败）: ", e);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "文件操作失败: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
    }
}
