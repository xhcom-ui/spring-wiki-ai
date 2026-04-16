package org.wiki.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LangChain4jService {

    private final ChatLanguageModel chatLanguageModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final Map<String, ChatMemory> chatMemories;

    @Autowired
    public LangChain4jService(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        this.embeddingStore = new InMemoryEmbeddingStore<>();
        this.chatMemories = new HashMap<>();
    }

    // 简单聊天
    public String chat(String message) {
        return chatLanguageModel.generate(message);
    }

    // 带记忆的聊天
    public String chatWithMemory(String sessionId, String message) {
        ChatMemory chatMemory = chatMemories.computeIfAbsent(sessionId, 
            k -> MessageWindowChatMemory.withMaxMessages(10));
        
        UserMessage userMessage = new UserMessage(message);
        chatMemory.add(userMessage);
        
        AiMessage aiMessage = chatLanguageModel.generate(chatMemory.messages()).content();
        chatMemory.add(aiMessage);
        
        return aiMessage.text();
    }

    // 清除聊天记忆
    public void clearMemory(String sessionId) {
        chatMemories.remove(sessionId);
    }

    // 使用 Prompt Template
    public String generateWithTemplate(String template, Map<String, Object> variables) {
        PromptTemplate promptTemplate = PromptTemplate.from(template);
        Prompt prompt = promptTemplate.apply(variables);
        return chatLanguageModel.generate(prompt.text());
    }

    // 添加文档到向量存储
    public void addDocument(String content) {
        Document document = Document.from(content);
        List<TextSegment> segments = splitDocument(document);
        
        for (TextSegment segment : segments) {
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
        }
    }

    // 基于文档的问答
    public String chatWithDocuments(String message) {
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .build();

        String relevantContent = contentRetriever.retrieve(dev.langchain4j.rag.query.Query.from(message))
                .stream()
                .map(content -> content.textSegment().text())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        String prompt = String.format(
            "基于以下信息回答问题。如果信息不足，请说明。\n\n相关信息:\n%s\n\n问题: %s",
            relevantContent, message
        );

        return chatLanguageModel.generate(prompt);
    }

    // 文本分析
    public String analyzeText(String text, String analysisType) {
        String template = """
            请对以下文本进行{{type}}分析：
            
            文本内容：
            {{text}}
            
            请提供详细的分析结果。""";
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("type", analysisType);
        variables.put("text", text);
        
        return generateWithTemplate(template, variables);
    }

    // 文本摘要
    public String summarize(String text, int maxLength) {
        String template = """
            请将以下文本总结为不超过{{maxLength}}字的摘要：
            
            文本内容：
            {{text}}
            
            摘要：""";
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("maxLength", maxLength);
        variables.put("text", text);
        
        return generateWithTemplate(template, variables);
    }

    // 文本翻译
    public String translate(String text, String targetLanguage) {
        String template = """
            请将以下文本翻译成{{language}}：
            
            原文：
            {{text}}
            
            译文：""";
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("language", targetLanguage);
        variables.put("text", text);
        
        return generateWithTemplate(template, variables);
    }

    // 代码生成
    public String generateCode(String description, String language) {
        String template = """
            请用{{language}}编写代码实现以下功能：
            
            功能描述：
            {{description}}
            
            请提供完整的代码，并包含必要的注释。""";
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("language", language);
        variables.put("description", description);
        
        return generateWithTemplate(template, variables);
    }

    // 创建新会话
    public String createSession() {
        return UUID.randomUUID().toString();
    }

    // 简单的文档分割
    private List<TextSegment> splitDocument(Document document) {
        String text = document.text();
        int chunkSize = 500;
        int overlap = 50;
        
        List<TextSegment> segments = new java.util.ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize - overlap) {
            int end = Math.min(i + chunkSize, text.length());
            segments.add(TextSegment.from(text.substring(i, end)));
            if (end == text.length()) break;
        }
        return segments;
    }
}
