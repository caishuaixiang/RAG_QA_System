package org.example.rag_qa_system.service;

/**
 * Embedding服务接口
 */
public interface EmbeddingService {

    /**
     * 生成文本向量
     * @param text 输入文本
     * @return 向量表示（JSON字符串）
     */
    String generateEmbedding(String text);
}