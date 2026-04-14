package org.example.rag_qa_system.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.rag_qa_system.entity.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 答案溯源信息工具类
 */
public class SourceInfo {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建答案来源信息
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
}