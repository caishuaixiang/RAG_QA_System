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
            source.put("similarity", Math.round(similarity * 10000.0) / 100.0);  // 保留两位小数，百分比形式

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

            // 章节标题
            if (chunk.getSectionTitle() != null && !chunk.getSectionTitle().isEmpty()) {
                location.put("section_title", chunk.getSectionTitle());
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