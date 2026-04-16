package org.wiki.service;

import cn.hutool.core.util.ReUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ContentExtractor {
    /**
     * 提取所有链接
     */
    public List<String> extractLinks(String html) {
        return ReUtil.findAllGroup1("href=[\"']([^\"']+)[\"']", html);
    }

    /**
     * 提取所有图片地址
     */
    public List<String> extractImages(String html) {
        return ReUtil.findAllGroup1("src=[\"']([^\"']+\\.(jpg|png|gif|webp))[\"']", html);
    }

    /**
     * 提取标签内容
     */
    public String extractTagContent(String html, String tagName) {
        String regex = "<" + tagName + "[^>]*>([^<]*)</" + tagName + ">";
        return ReUtil.getGroup1(regex, html);
    }

    /**
     * 提取所有指定标签的内容
     */
    public List<String> extractAllTagContent(String html, String tagName) {
        String regex = "<" + tagName + "[^>]*>([^<]*)</" + tagName + ">";
        return ReUtil.findAllGroup1(regex, html);
    }

    /**
     * 移除HTML标签，保留纯文本
     */
    public String stripHtml(String html) {
        // 先删除script和style标签及其内容
        String result = ReUtil.delAll("<script[^>]*>[\\s\\S]*?</script>", html);
        result = ReUtil.delAll("<style[^>]*>[\\s\\S]*?</style>", result);
        // 删除所有标签
        result = ReUtil.delAll("<[^>]+>", result);
        // 压缩空白
        result = ReUtil.replaceAll(result, "\\s+", " ");
        return result.trim();
    }

    /**
     * 提取JSON中的字段值
     */
    public String extractJsonValue(String json, String key) {
        String regex = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        return ReUtil.getGroup1(regex, json);
    }


    private static final Map<String, Pattern> PATTERNS = new HashMap<>();

    static {
        PATTERNS.put("phone", Pattern.compile("^1[3-9]\\d{9}$"));
        PATTERNS.put("email", Pattern.compile("^\\w+([.-]?\\w+)*@\\w+([.-]?\\w+)*\\.\\w{2,}$"));
        PATTERNS.put("idCard", Pattern.compile("^\\d{17}[\\dXx]$"));
        PATTERNS.put("username", Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{3,15}$"));
        PATTERNS.put("password", Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@#$%]{8,20}$"));
        PATTERNS.put("ip", Pattern.compile("^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$"));
        PATTERNS.put("url", Pattern.compile("^https?://[\\w.-]+(/\\S*)?$"));
    }

    /**
     * 验证字段
     */
    public boolean validate(String type, String value) {
        Pattern pattern = PATTERNS.get(type);
        if (pattern == null) {
            throw new IllegalArgumentException("未知验证类型: " + type);
        }
        return ReUtil.isMatch(pattern, value);
    }

    /**
     * 批量验证
     */
    public Map<String, Boolean> validateAll(Map<String, String> fields) {
        Map<String, Boolean> result = new HashMap<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String[] parts = entry.getKey().split(":");
            if (parts.length == 2) {
                result.put(parts[0], validate(parts[1], entry.getValue()));
            }
        }
        return result;
    }

    /**
     * 密码强度检查
     */
    public int checkPasswordStrength(String password) {
        int strength = 0;

        if (password.length() >= 8) strength++;
        if (ReUtil.contains("[a-z]", password)) strength++;
        if (ReUtil.contains("[A-Z]", password)) strength++;
        if (ReUtil.contains("\\d", password)) strength++;
        if (ReUtil.contains("[!@#$%^&*]", password)) strength++;

        return strength;
    }

    public static void main(String[] args) {
        ContentExtractor extractor = new ContentExtractor();
        String html = "<html><head><title>测试页面</title></head>" +
                "<body><a href=\"http://example.com\">链接1</a>" +
                "<img src=\"logo.png\"/><p>段落内容</p></body></html>";

        // 提取链接
        List<String> links = extractor.extractLinks(html);
        System.out.println("链接: " + links);  // [http://example.com]

        // 提取标题
        String title = extractor.extractTagContent(html, "title");
        System.out.println("标题: " + title);  // 测试页面

        // 纯文本
        String text = extractor.stripHtml(html);
        System.out.println("纯文本: " + text);
    }
}