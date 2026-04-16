package org.wiki.controller;

import org.wiki.entity.Token;
import org.wiki.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tokens")
@CrossOrigin(origins = "*") // 允许跨域请求，便于前端访问
public class TokenController {

    @Autowired
    private TokenService tokenService;

    // 记录 token 使用
    @PostMapping("/record")
    public void recordTokenUsage(@RequestBody Map<String, Object> request) {
        String type = (String) request.get("type");
        int count = (Integer) request.get("count");
        tokenService.recordTokenUsage(type, count);
    }

    // 获取所有 token 使用记录
    @GetMapping("/all")
    public List<Token> getAllTokenUsage() {
        return tokenService.getAllTokenUsage();
    }

    // 获取指定时间范围内的 token 使用记录
    @GetMapping("/range")
    public List<Token> getTokenUsageByRange(
            @RequestParam("start") String start, 
            @RequestParam("end") String end) {
        LocalDateTime startDateTime = LocalDateTime.parse(start);
        LocalDateTime endDateTime = LocalDateTime.parse(end);
        return tokenService.getTokenUsage(startDateTime, endDateTime);
    }

    // 获取统计信息
    @GetMapping("/stats")
    public Map<String, Object> getTokenStats() {
        List<Token> allTokens = tokenService.getAllTokenUsage();
        double totalCost = tokenService.calculateTotalCost(allTokens);
        int totalTokens = tokenService.calculateTotalTokens(allTokens);

        return Map.of(
                "totalTokens", totalTokens,
                "totalCost", totalCost,
                "usageCount", allTokens.size()
        );
    }

    /**
     * 统计单段文本
     */
    @PostMapping("/count")
    public Map<String, Object> count(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        int tokens = tokenService.countSingleToken(text);
        return Map.of(
                "text", text,
                "tokenCount", tokens
        );
    }

    /**
     * 统计对话
     */
    @PostMapping("/conversation")
    public Map<String, Object> countConversation(@RequestBody Map<String, List<String>> request) {
        List<String> messages = request.get("messages");
        int total = tokenService.countConversationTokens(messages);
        return Map.of(
                "messageCount", messages.size(),
                "totalTokens", total
        );
    }

    /**
     * 查看统计日志
     */
    @GetMapping("/logs")
    public List<TokenLog> logs() {
        return tokenService.getLogs();
    }
}
