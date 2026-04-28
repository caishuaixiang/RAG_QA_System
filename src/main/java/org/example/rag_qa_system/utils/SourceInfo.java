package org.example.rag_qa_system.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.rag_qa_system.entity.Document;
import org.example.rag_qa_system.entity.DocumentChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 答案溯源信息工具类
 * 支持详细的来源定位，包括：
 * - 文档名称和ID
 * - 切片在原文中的位置（起始/结束字符位置）
 * - 页码信息（对于PDF等分页文档）
 * - 章节标题
 * - 段落序号
 * - 行号范围
 */
public class SourceInfo {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建答案来源信息（基础版本，兼容旧代码）
     * @param documents 相关文档列表
     * @param similarities 相似度列表
     * @return 来源信息JSON字符串
     */
    public static String createSourceInfo(List<Document> documents, List<Double> similarities) {
        List<Map<String, Object>> sources = new ArrayList<>();

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            double similarity = similarities.get(i);

            Map<String, Object> source = new HashMap<>();
            source.put("document_id", doc.getId());
            source.put("document_name", doc.getName());
            source.put("knowledge_domain", doc.getKnowledgeDomain());
            source.put("similarity", similarity);
            source.put("source_type", "document");

            sources.add(source);
        }

        try {
            return objectMapper.writeValueAsString(sources);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * 创建详细的答案来源信息（包含位置溯源）
     * @param chunks 相关文档切片列表
     * @param documents 文档信息映射（documentId -> Document）
     * @param similarities 相似度列表
     * @return 来源信息JSON字符串
     */
    public static String createDetailedSourceInfo(List<DocumentChunk> chunks,
                                                  Map<Long, Document> documents,
                                                  List<Double> similarities) {
        List<Map<String, Object>> sources = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            double similarity = similarities.get(i);
            Document doc = documents.get(chunk.getDocumentId());

            Map<String, Object> source = new HashMap<>();

            // 文档基本信息
            source.put("document_id", chunk.getDocumentId());
            source.put("document_name", doc != null ? doc.getName() : "未知文档");
            source.put("knowledge_domain", doc != null ? doc.getKnowledgeDomain() : "未知领域");
            source.put("chunk_index", chunk.getChunkIndex());
            source.put("similarity", Math.round(similarity * 100.0) / 100.0);  // 保留两位小数，百分比形式

            // 位置溯源信息
            Map<String, Object> location = new HashMap<>();

            // 字符位置
            if (chunk.getStartPosition() != null && chunk.getEndPosition() != null) {
                location.put("char_range", chunk.getStartPosition() + "-" + chunk.getEndPosition());
                location.put("start_char", chunk.getStartPosition());
                location.put("end_char", chunk.getEndPosition());
            }

            // 页码（对于PDF等分页文档）
            if (chunk.getPageNumber() != null) {
                location.put("page_number", chunk.getPageNumber());
            }

            // 章节标题 - 优先使用有效的标题
            String sectionTitle = extractValidTitle(chunk.getSectionTitle(), chunk.getChunkContent());
            if (sectionTitle != null && !sectionTitle.isEmpty()) {
                location.put("section_title", sectionTitle);
            }

            // 段落序号
            if (chunk.getParagraphIndex() != null) {
                location.put("paragraph_index", chunk.getParagraphIndex());
            }

            // 行号范围
            if (chunk.getLineRange() != null && !chunk.getLineRange().isEmpty()) {
                location.put("line_range", chunk.getLineRange());
            }

            source.put("location", location);

            // 切片内容预览（前100字符）
            String content = chunk.getChunkContent();
            if (content != null && content.length() > 100) {
                source.put("content_preview", content.substring(0, 100) + "...");
            } else {
                source.put("content_preview", content);
            }

            sources.add(source);
        }

        try {
            return objectMapper.writeValueAsString(sources);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * 提取有效的标题
     * 优先级：1. 有效的章节标题 2. 从内容中提取的标题 3. null
     */
    private static String extractValidTitle(String sectionTitle, String content) {
        // 如果章节标题有效，直接使用
        if (isValidTitle(sectionTitle)) {
            return sectionTitle;
        }

        // 尝试从内容中提取标题
        if (content != null && !content.isEmpty()) {
            String extractedTitle = extractTitleFromContent(content);
            if (extractedTitle != null) {
                return extractedTitle;
            }
        }

        return null;
    }

    /**
     * 判断标题是否有效
     */
    private static boolean isValidTitle(String title) {
        if (title == null || title.isEmpty()) {
            return false;
        }

        // 标题长度限制（太长不是标题）
        if (title.length() > 50) {
            return false;
        }

        // 标题不应以标点符号结尾（除顿号、书名号）
        char lastChar = title.charAt(title.length() - 1);
        if ("。！？，；：、.!?,:;".indexOf(lastChar) >= 0) {
            return false;
        }

        // 包含这些关键词的更可能是有效标题
        String[] keywords = {
                "制度", "规定", "办法", "条例", "守则", "手册", "指南", "须知",
                "章程", "细则", "意见", "通知", "决定", "方案", "措施",
                "第一章", "第二章", "第三章", "第四章", "第五章", "第六章", "第七章", "第八章", "第九章", "第十章",
                "第一节", "第二节", "第三节", "第四节", "第五节",
                "附件", "附录", "附则", "条", "款"
        };

        for (String keyword : keywords) {
            if (title.contains(keyword)) {
                return true;
            }
        }

        // 数字编号开头（如"1."、"一、"）
        if (title.matches("^[一二三四五六七八九十]+、.*") ||
                title.matches("^\\d+[\\.、．].*") ||
                title.matches("^第[一二三四五六七八九十百千万零\\d]+[章节篇部].*")) {
            return true;
        }

        return false;
    }

    /**
     * 从内容中提取标题
     */
    private static String extractTitleFromContent(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        // 尝试匹配常见的标题模式
        String[] lines = content.split("\\r?\\n");

        for (String line : lines) {
            String trimmedLine = line.trim();

            // 跳过空行和太短的行
            if (trimmedLine.isEmpty() || trimmedLine.length() < 5) {
                continue;
            }

            // 检查是否是手册名称格式（如"西安科技大学研究生请假制度"）
            if (trimmedLine.matches(".*[大学学院学校].*[制度规定办法条例守则手册指南].*")) {
                // 确保不是太长
                if (trimmedLine.length() <= 50) {
                    return trimmedLine;
                }
            }

            // 检查是否包含年份的标题（如"（2017年8月修订）"）
            if (trimmedLine.matches(".*[（(].*[0-9]{4}.*年.*[修订发布施行].*[）)].*")) {
                // 继续查找上一行作为主标题
                continue;
            }

            // 检查是否以关键词结尾
            String[] endKeywords = {"制度", "规定", "办法", "条例", "守则", "手册", "指南", "须知"};
            for (String keyword : endKeywords) {
                if (trimmedLine.endsWith(keyword) && trimmedLine.length() <= 50) {
                    return trimmedLine;
                }
            }
        }

        return null;
    }

    /**
     * 创建人类可读的来源描述
     * @param chunk 文档切片
     * @param document 文档信息
     * @return 可读的来源描述字符串
     */
    public static String createReadableSourceDescription(DocumentChunk chunk, Document document) {
        StringBuilder sb = new StringBuilder();

        sb.append("文档：").append(document.getName());

        // 页码信息
        if (chunk.getPageNumber() != null) {
            sb.append("，第").append(chunk.getPageNumber()).append("页");
        }

        // 章节信息
        if (chunk.getSectionTitle() != null && !chunk.getSectionTitle().isEmpty()) {
            sb.append("，章节：").append(chunk.getSectionTitle());
        }

        // 段落信息
        if (chunk.getParagraphIndex() != null) {
            sb.append("，第").append(chunk.getParagraphIndex() + 1).append("段");
        }

        // 行号信息
        if (chunk.getLineRange() != null && !chunk.getLineRange().isEmpty()) {
            sb.append("，第").append(chunk.getLineRange()).append("行");
        }

        // 字符位置
        if (chunk.getStartPosition() != null && chunk.getEndPosition() != null) {
            sb.append("，字符位置：").append(chunk.getStartPosition()).append("-").append(chunk.getEndPosition());
        }

        return sb.toString();
    }
    /**
     * 创建相关文档ID列表
     * @param documents 相关文档列表
     * @return 文档ID列表JSON字符串
     */
    public static String createRelatedDocumentIds(List<Document> documents) {
        List<Long> docIds = new ArrayList<>();
        for (Document doc : documents) {
            docIds.add(doc.getId());
        }

        try {
            return objectMapper.writeValueAsString(docIds);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * 创建相关切片ID列表
     * @param chunks 相关切片列表
     * @return 切片ID列表JSON字符串
     */
    public static String createRelatedChunkIds(List<DocumentChunk> chunks) {
        List<Long> chunkIds = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            chunkIds.add(chunk.getId());
        }

        try {
            return objectMapper.writeValueAsString(chunkIds);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * 解析来源信息
     * @param sourceInfo 来源信息JSON字符串
     * @return 来源信息列表
     */
    public static List<Map<String, Object>> parseSourceInfo(String sourceInfo) {
        try {
            return objectMapper.readValue(sourceInfo, List.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 格式化来源信息为可读文本
     * @param sourceInfo 来源信息JSON字符串
     * @return 格式化的可读文本
     */
    public static String formatSourceInfoAsText(String sourceInfo) {
        List<Map<String, Object>> sources = parseSourceInfo(sourceInfo);
        if (sources.isEmpty()) {
            return "暂无来源信息";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("答案来源：\n");

        for (int i = 0; i < sources.size(); i++) {
            Map<String, Object> source = sources.get(i);
            sb.append("\n【来源 ").append(i + 1).append("】\n");
            sb.append("  文档：").append(source.get("document_name")).append("\n");

            Object similarity = source.get("similarity");
            if (similarity != null) {
                sb.append("  相关度：").append(similarity).append("%\n");
            }

            Object location = source.get("location");
            if (location instanceof Map) {
                Map<?, ?> loc = (Map<?, ?>) location;

                if (loc.get("page_number") != null) {
                    sb.append("  页码：第").append(loc.get("page_number")).append("页\n");
                }
                if (loc.get("section_title") != null) {
                    sb.append("  章节：").append(loc.get("section_title")).append("\n");
                }
                if (loc.get("paragraph_index") != null) {
                    sb.append("  段落：第").append((Integer) loc.get("paragraph_index") + 1).append("段\n");
                }
                if (loc.get("line_range") != null) {
                    sb.append("  行号：第").append(loc.get("line_range")).append("行\n");
                }
                if (loc.get("char_range") != null) {
                    sb.append("  字符位置：").append(loc.get("char_range")).append("\n");
                }
            }
        }

        return sb.toString();
    }
}