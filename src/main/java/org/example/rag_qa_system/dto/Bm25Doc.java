package org.example.rag_qa_system.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * BM25检索结果文档
 */
@Data
@Accessors(chain = true)
public class Bm25Doc {

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
     * BM25得分
     */
    private float bm25Score;
}
