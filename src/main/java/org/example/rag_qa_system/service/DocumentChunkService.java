package org.example.rag_qa_system.service;

import org.example.rag_qa_system.entity.DocumentChunk;
import org.example.rag_qa_system.utils.DocumentParser;
import org.example.rag_qa_system.utils.TextChunker;

import java.util.List;

/**
 * 文档切片服务接口
 */
public interface DocumentChunkService {

    /**
     * 创建文档切片（基础方法，不包含位置信息）
     * @param documentId 文档ID
     * @param chunks 切片列表
     */
    void createChunks(Long documentId, List<String> chunks);

    /**
     * 创建文档切片（增强方法，包含位置溯源信息）
     * @param documentId 文档ID
     * @param chunkResults 切片结果列表（包含位置信息）
     * @param sections 章节信息列表
     */
    void createChunksWithLocation(Long documentId, List<TextChunker.ChunkResult> chunkResults,
                                  List<TextChunker.SectionInfo> sections);

    /**
     * 创建文档切片（增强方法，包含位置溯源信息和页码）
     * @param documentId 文档ID
     * @param chunkResults 切片结果列表（包含位置信息）
     * @param sections 章节信息列表
     * @param pagePositions 页码位置列表（PDF文档）
     */
    void createChunksWithLocation(Long documentId, List<TextChunker.ChunkResult> chunkResults,
                                  List<TextChunker.SectionInfo> sections,
                                  List<DocumentParser.PagePosition> pagePositions);

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