package org.example.rag_qa_system.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 混合检索结果文档（RRF融合后）
 */
@Data
@Accessors(chain = true)
public class HybridDoc {

    /**
     * 文档切片ID
     */
    private String docId;

    /**
     * 切片内容
     */
    private String content;

    /**
     * 来源文档ID
     */
    private Long sourceDocId;

    /**
     * RRF融合得分（用于排序）
     */
    private double rrfScore;

    /**
     * 向量相似度（用于展示，0-100%）
     */
    private double vectorSimilarity;

    /**
     * 是否由向量检索命中
     */
    private boolean vectorHit;
}
