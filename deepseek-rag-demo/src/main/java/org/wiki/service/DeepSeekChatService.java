package org.wiki.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * DeepSeek 本地对话服务
 * 通过 Spring AI 框架直接调用 DeepSeek API（兼容 OpenAI 接口）
 * 支持纯对话模式、RAG 增强模式和流式输出
 */
@Slf4j
@Service
public class DeepSeekChatService {

    private final ChatClient chatClient;

    public DeepSeekChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 纯对话模式 - 直接与 DeepSeek 大模型对话
     *
     * @param question 用户问题
     * @return 模型回答
     */
    public String chat(String question) {
        log.info("DeepSeek 对话请求: question={}", question);
        String answer = chatClient.prompt()
                .user(question)
                .call()
                .content();
        log.info("DeepSeek 对话响应: answer={}", answer != null ? answer.substring(0, Math.min(100, answer.length())) : "null");
        return answer;
    }

    /**
     * RAG 增强对话模式 - 基于检索到的上下文进行回答
     * 先从向量库检索相关知识片段，再传给 DeepSeek 生成回答
     *
     * @param question    用户问题
     * @param context     从知识库检索到的上下文内容
     * @return 模型回答
     */
    public String chatWithContext(String question, String context) {
        log.info("DeepSeek RAG 对话请求: question={}, contextLength={}", question, context != null ? context.length() : 0);

        String systemPrompt = """
                你是一个专业的知识库问答助手，请根据提供的上下文内容回答用户的问题。
                
                回答要求：
                1. 只使用上下文中提供的信息，不要编造答案
                2. 如果上下文中没有相关信息，请明确告知用户"根据提供的资料无法回答该问题"
                3. 回答要准确、完整、有条理
                4. 可以适当引用上下文中的原文
                5. 使用中文回答
                
                上下文内容：
                %s
                """.formatted(context);

        String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();
        log.info("DeepSeek RAG 对话响应: answer={}", answer != null ? answer.substring(0, Math.min(100, answer.length())) : "null");
        return answer;
    }

    /**
     * 流式对话模式 - 使用 Spring AI 原生 Flux 流式输出
     *
     * @param question 用户问题
     * @return Flux 流式回答
     */
    public Flux<String> chatStream(String question) {
        log.info("DeepSeek 流式对话请求: question={}", question);
        return chatClient.prompt()
                .user(question)
                .stream()
                .content();
    }

    /**
     * RAG 增强流式对话模式
     *
     * @param question 用户问题
     * @param context  知识库检索上下文
     * @return Flux 流式回答
     */
    public Flux<String> chatStreamWithContext(String question, String context) {
        log.info("DeepSeek RAG 流式对话请求: question={}, contextLength={}", question, context != null ? context.length() : 0);

        String systemPrompt = """
                你是一个专业的知识库问答助手，请根据提供的上下文内容回答用户的问题。
                
                回答要求：
                1. 只使用上下文中提供的信息，不要编造答案
                2. 如果上下文中没有相关信息，请明确告知用户"根据提供的资料无法回答该问题"
                3. 回答要准确、完整、有条理
                4. 可以适当引用上下文中的原文
                5. 使用中文回答
                
                上下文内容：
                %s
                """.formatted(context);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .stream()
                .content();
    }
}
