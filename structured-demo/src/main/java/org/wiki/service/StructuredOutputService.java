package org.wiki.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.wiki.entity.ActorFilms;
import org.wiki.entity.Address;
import org.wiki.entity.CodeReview;
import org.wiki.entity.MovieRecommendation;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class StructuredOutputService {
    
    private final ChatClient chatClient;
    
    // 1. 提取地址信息
    public Address extractAddress(String text) {
        return chatClient.prompt()
                .user(userSpec -> userSpec
                    .text("请从以下文本中提取完整的收货地址信息：{text}")
                    .param("text", text))
                .advisors((Consumer<ChatClient.AdvisorSpec>) new SimpleLoggerAdvisor())
                .call()
                .entity(Address.class);
    }
    
    // 2. 获取演员电影作品列表
    public ActorFilms getActorFilmography(String actor) {
        BeanOutputConverter<ActorFilms> converter = 
            new BeanOutputConverter<>(ActorFilms.class);
        
        String format = converter.getFormat();
        
        return chatClient.prompt()
                .system(systemSpec -> systemSpec
                    .text("""
                        你是一位电影专家。请为指定的演员生成电影作品列表。
                        请严格按照以下格式返回：
                        {format}
                        """))
                .user(userSpec -> userSpec
                    .text("请生成演员 {actor} 的电影作品列表，包含至少5部电影")
                    .param("actor", actor))
                .advisors((Consumer<ChatClient.AdvisorSpec>) new SimpleLoggerAdvisor())
                .call()
                .entity(ActorFilms.class);
    }
    
    // 3. 获取电影推荐
    public MovieRecommendation recommendMovie(String genre, String language) {
        return chatClient.prompt()
                .system("""
                    你是一位专业的电影推荐专家。请根据用户的偏好推荐电影。
                    返回的电影信息必须包含：标题、发行年份、导演、类型、语言、评分、时长、简介和主要演员。
                    """)
                .user(userSpec -> userSpec
                    .text("请推荐一部{genre}类型的{language}电影，并提供详细信息")
                    .param("genre", genre)
                    .param("language", language))
                .advisors((Consumer<ChatClient.AdvisorSpec>) new SimpleLoggerAdvisor())
                .call()
                .entity(MovieRecommendation.class);
    }
    
    // 4. 代码审查
    public CodeReview reviewCode(String code) {
        BeanOutputConverter<CodeReview> converter = 
            new BeanOutputConverter<>(CodeReview.class);
        
        String format = converter.getFormat();
        
        return chatClient.prompt()
                .system(systemSpec -> systemSpec
                    .text("""
                        你是一位高级软件工程师。请审查以下代码，指出问题并提供建议。
                        请严格按照以下格式返回：
                        {format}
                        """))
                .user(userSpec -> userSpec
                    .text("请审查以下代码：\n{code}")
                    .param("code", code))
                .advisors((Consumer<ChatClient.AdvisorSpec>) new SimpleLoggerAdvisor())
                .call()
                .entity(CodeReview.class);
    }
    
    // 5. Map输出转换器示例
    public Map<String, Object> extractPersonInfo(String description) {
        MapOutputConverter converter = new MapOutputConverter();
        
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate("""
            请从以下描述中提取个人信息，并按照指定格式返回：
            需要的字段：name, age, occupation, city, interests
            {format}
            """);
        
        var systemMessage = systemPromptTemplate.createMessage(
            Map.of("format", converter.getFormat())
        );
        
        return chatClient.prompt()
                .system(String.valueOf(systemMessage))
                .user(userSpec -> userSpec
                    .text("描述：{description}")
                    .param("description", description))
                .call()
                .entity(Map.class);
    }
    
    // 6. List输出转换器示例
    public List<String> extractKeywords(String text) {
        //ListOutputConverter converter = new ListOutputConverter();
        
        return chatClient.prompt()
                .system("请从文本中提取关键词，返回一个列表")
                .user(userSpec -> userSpec
                    .text("请从以下文本中提取5-10个关键词：\n{text}")
                    .param("text", text))
                .advisors((Consumer<ChatClient.AdvisorSpec>) new SimpleLoggerAdvisor())
                .call()
                .entity(new ParameterizedTypeReference<List<String>>() {});
    }
    
    // 7. 复合结构化输出示例
    public Map<String, List<String>> categorizeBooks(List<String> bookTitles) {
        String format = """
            {
                "fiction": ["小说1", "小说2"],
                "nonfiction": ["非小说1", "非小说2"],
                "science": ["科学书籍1", "科学书籍2"]
            }
            """;
        
        return chatClient.prompt()
                .system("请将书籍分类，返回JSON格式")
                .user(userSpec -> userSpec
                    .text("""
                        请将以下书籍分类：
                        {bookTitles}
                        
                        分类为：fiction（小说类）、nonfiction（非小说类）、science（科学类）
                        """)
                    .param("bookTitles", String.join(", ", bookTitles)))
                .advisors((Consumer<ChatClient.AdvisorSpec>) new SimpleLoggerAdvisor())
                .call()
                .entity(new ParameterizedTypeReference<Map<String, List<String>>>() {});
    }
    
    // 8. 流式响应结构化输出
    public String streamStructuredResponse(String query) {
        StringBuilder response = new StringBuilder();
        
        chatClient.prompt()
                .system("请以JSON格式返回查询结果")
                .user(query)
                .stream()
                .content()
                .toStream()
                .forEach(content -> {
                    response.append(content);
                    System.out.print(content);
                });
        
        return response.toString();
    }
}