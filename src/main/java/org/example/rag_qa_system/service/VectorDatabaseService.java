package org.example.rag_qa_system.service;

import org.example.rag_qa_system.dto.SearchResult;
import org.example.rag_qa_system.entity.DocumentChunk;

import java.util.List;

/**
 * 向量数据库服务接口
 */
public interface VectorDatabaseService {

    /**
     * 添加文档切片到向量数据库
     * @param documentId 文档ID
     * @param chunks 文档切片列表
     */
    void addChunksToVectorDB(Long documentId, List<DocumentChunk> chunks);

    /**
     * 检索相关切片
     * @param queryVector 查询向量
     * @param topK 返回前K个结果
     * @return 相关切片列表
     */
    List<DocumentChunk> searchSimilarChunks(float[] queryVector, int topK);

    /**
     * 检索相关切片（带距离值）
     * @param queryVector 查询向量
     * @param topK 返回前K个结果
     * @return 搜索结果列表（包含切片和距离）
     */
    List<SearchResult> searchSimilarChunksWithDistance(float[] queryVector, int topK);

    /**
     * 删除文档切片
     * @param documentId 文档ID
     */
    void deleteChunksFromVectorDB(Long documentId);
}