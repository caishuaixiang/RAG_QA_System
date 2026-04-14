package org.example.rag_qa_system.service;

import org.example.rag_qa_system.entity.DocumentChunk;

import java.util.List;

/**
 * 文档切片服务接口
 */
public interface DocumentChunkService {

    /**
     * 创建文档切片
     * @param documentId 文档ID
     * @param chunks 切片列表
     */
    void createChunks(Long documentId, List<String> chunks);

    /**
     * 获取文档切片
     * @param documentId 文档ID
     * @return 切片列表
     */
    List<DocumentChunk> getChunksByDocumentId(Long documentId);

    /**
     * 根据文档ID和标签获取切片
     * @param documentId 文档ID
     * @param tags 标签
     * @return 切片列表
     */
    List<DocumentChunk> getChunksByDocumentIdAndTags(Long documentId, String tags);

    /**
     * 批量保存切片
     * @param chunks 切片列表
     */
    void batchSaveChunks(List<DocumentChunk> chunks);

    /**
     * 删除文档切片
     * @param documentId 文档ID
     */
    void deleteChunksByDocumentId(Long documentId);

    /**
     * 根据ID删除切片
     * @param chunkId 切片ID
     */
    void deleteChunkById(Long chunkId);

    /**
     * 更新切片
     * @param chunk 切片信息
     */
    void updateDocument(DocumentChunk chunk);
}