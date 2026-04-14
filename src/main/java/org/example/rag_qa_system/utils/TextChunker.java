package org.example.rag_qa_system.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本切片工具类
 */
public class TextChunker {

    /**
     * 按固定长度切片
     * @param text 文本内容
     * @param chunkSize 切片大小
     * @return 切片列表
     */
    public static List<String> splitByLength(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, text.length());
            chunks.add(text.substring(i, end));
        }
        return chunks;
    }

    /**
     * 按段落切片（基于换行符）
     * @param text 文本内容
     * @return 切片列表
     */
    public static List<String> splitByParagraph(String text) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\n+");
        for (String paragraph : paragraphs) {
            if (!paragraph.trim().isEmpty()) {
                chunks.add(paragraph.trim());
            }
        }
        return chunks;
    }

    /**
     * 按句子切片
     * @param text 文本内容
     * @return 切片列表
     */
    public static List<String> splitBySentence(String text) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = text.split("[。！？]\\s*");
        for (String sentence : sentences) {
            if (!sentence.trim().isEmpty()) {
                chunks.add(sentence.trim() + "。");
            }
        }
        return chunks;
    }

    /**
     * 智能切片（结合多种策略）
     * @param text 文本内容
     * @param maxLength 最大切片大小
     * @return 切片列表
     */
    public static List<String> smartSplit(String text, int maxLength) {
        List<String> chunks = new ArrayList<>();

        // 首先按段落分割
        List<String> paragraphs = splitByParagraph(text);

        for (String paragraph : paragraphs) {
            if (paragraph.length() <= maxLength) {
                chunks.add(paragraph);
            } else {
                // 段落太长，按句子分割
                List<String> sentences = splitBySentence(paragraph);
                StringBuilder currentChunk = new StringBuilder();

                for (String sentence : sentences) {
                    if (currentChunk.length() + sentence.length() <= maxLength) {
                        currentChunk.append(sentence);
                    } else {
                        if (currentChunk.length() > 0) {
                            chunks.add(currentChunk.toString());
                            currentChunk = new StringBuilder();
                        }
                        currentChunk.append(sentence);
                    }
                }

                // 处理单个超长句子（强制分割）
                if (currentChunk.length() > maxLength) {
                    String remaining = currentChunk.toString();
                    while (remaining.length() > maxLength) {
                        chunks.add(remaining.substring(0, maxLength));
                        remaining = remaining.substring(maxLength);
                    }
                    if (remaining.length() > 0) {
                        chunks.add(remaining);
                    }
                } else if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                }
            }
        }

        return chunks;
    }
}