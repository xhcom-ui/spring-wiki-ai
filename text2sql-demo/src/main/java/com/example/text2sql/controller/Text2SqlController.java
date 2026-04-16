package com.example.text2sql.controller;

import com.example.text2sql.service.Text2SqlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/text2sql")
public class Text2SqlController {

    @Autowired
    private Text2SqlService text2SqlService;

    @PostMapping("/convert")
    public ResponseEntity<Map<String, Object>> convertTextToSql(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            if (text == null || text.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "查询文本不能为空"));
            }
            Map<String, Object> result = text2SqlService.convertTextToSql(text);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
