package com.xxx.rag.rag.splitter;

import com.xxx.rag.common.constant.RagConstant;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本分块器
 */
public class TextSplitter {

    /**
     * 分块文本
     */
    public static List<String> splitText(String text) {
        return splitText(text, RagConstant.CHUNK_SIZE, RagConstant.CHUNK_OVERLAP);
    }

    /**
     * 分块文本
     */
    public static List<String> splitText(String text, int chunkSize, int chunkOverlap) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        int end = chunkSize;

        while (start < text.length()) {
            // 确保不超过文本长度
            if (end > text.length()) {
                end = text.length();
            }

            // 尝试在句子边界分割
            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf('.', end);
                int lastComma = text.lastIndexOf(',', end);
                int lastSpace = text.lastIndexOf(' ', end);

                if (lastPeriod > start + chunkOverlap) {
                    end = lastPeriod + 1;
                } else if (lastComma > start + chunkOverlap) {
                    end = lastComma + 1;
                } else if (lastSpace > start + chunkOverlap) {
                    end = lastSpace;
                }
            }

            // 添加分块
            chunks.add(text.substring(start, end).trim());

            // 移动到下一个分块，考虑重叠
            start = end - chunkOverlap;
            end = start + chunkSize;
        }

        return chunks;
    }
}
