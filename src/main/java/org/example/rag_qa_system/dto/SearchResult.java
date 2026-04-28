package org.example.rag_qa_system.dto;

import lombok.Data;
import org.example.rag_qa_system.entity.DocumentChunk;

/**
 * 向量搜索结果封装类
 * 包含文档切片和相似度距离
 */
@Data
public class SearchResult {
    /**
     * 文档切片
     */
    private DocumentChunk chunk;

    /**
     * 距离值（越小越相似，ChromaDB返回的距离）
     */
    private double distance;

    public SearchResult(DocumentChunk chunk, double distance) {
        this.chunk = chunk;
        this.distance = distance;
    }

    /**
     * 将距离转换为相似度百分比
     * ChromaDB默认使用L2距离，距离范围通常是0-2
     * 相似度 = (1 - distance/2) * 100，范围0-100%
     */
    public double getSimilarityPercentage() {
        // 假设距离范围是0-2，转换为0-100%的相似度
        double similarity = 1.0 - (distance / 2.0);
        // 确保在0-1范围内
        similarity = Math.max(0, Math.min(1, similarity));
        return Math.round(similarity * 10000.0) / 100.0; // 保留两位小数
    }
}