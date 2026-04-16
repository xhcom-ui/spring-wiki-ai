package org.wiki.service;

import jakarta.annotation.PostConstruct;
import org.wiki.entity.Token;
import org.wiki.entity.TokenLog;
import org.wiki.repository.TokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;


import java.time.LocalDateTime;
import java.util.List;

@Service
public class TokenService {
    @Autowired
    private TokenRepository tokenRepository;
    //private final TokenLogRepository logRepository;
    private Encoding encoding;

    // 模拟 token 使用记录
    public void recordTokenUsage(String type, int count) {
        Token token = new Token();
        token.setType(type);
        token.setCount(count);
        // 简单的费用计算逻辑，实际应用中可能需要更复杂的计算
        double cost = count * 0.001; // 假设每个 token 0.001 元
        token.setCost(cost);
        token.setTimestamp(LocalDateTime.now());
        tokenRepository.save(token);
    }

    // 获取指定时间范围内的 token 使用记录
    public List<Token> getTokenUsage(LocalDateTime start, LocalDateTime end) {
        return tokenRepository.findByTimestampBetween(start, end);
    }

    // 获取所有 token 使用记录
    public List<Token> getAllTokenUsage() {
        return tokenRepository.findAll();
    }

    // 计算总费用
    public double calculateTotalCost(List<Token> tokens) {
        return tokens.stream().mapToDouble(Token::getCost).sum();
    }

    // 计算总 token 数
    public int calculateTotalTokens(List<Token> tokens) {
        return tokens.stream().mapToInt(Token::getCount).sum();
    }



    // 初始化 Token 编码器（GPT3.5 / GPT4 标准）
    @PostConstruct
    public void init() {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        this.encoding = registry.getEncodingForModel(ModelType.GPT_4O);
    }

    /**
     * 统计单段文本 Token
     */
    public int countSingleToken(String text) {
        int tokens = encoding.countTokens(text);

        // 保存日志
//        TokenLog log = new TokenLog();
//        log.setContent(text);
//        log.setTokenCount(tokens);
//        log.setType("single");
//        log.setCreateTime(LocalDateTime.now());
//        logRepository.save(log);

        return tokens;
    }

    /**
     * 统计对话总 Token
     */
    public int countConversationTokens(List<String> messages) {
        int total = 0;
        for (String msg : messages) {
            total += encoding.countTokens(msg);
        }

        // 日志
//        TokenLog log = new TokenLog();
//        log.setContent("对话共" + messages.size() + "条");
//        log.setTokenCount(total);
//        log.setType("conversation");
//        log.setCreateTime(LocalDateTime.now());
//        logRepository.save(log);

        return total;
    }

    /**
     * 查询所有统计记录
     */
    public List<TokenLog> getLogs() {
        //return logRepository.findAll();
        return List.of();
    }
}
