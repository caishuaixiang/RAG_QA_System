package org.example.rag_qa_system.service;

import org.example.rag_qa_system.entity.Document;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档服务接口
 */
public interface DocumentService {

    /**
     * 上传文档
     * @param file 文档文件
     * @param knowledgeDomain 知识域分类
     * @return 文档信息
     */
    Document uploadDocument(MultipartFile file, String knowledgeDomain) throws Exception;

    /**
     * 上传文档（带用户ID）
     * @param file 文档文件
     * @param knowledgeDomain 知识域分类
     * @param userId 用户ID
     * @return 文档信息
     */
    Document uploadDocument(MultipartFile file, String knowledgeDomain, Long userId) throws Exception;

    /**
     * 获取文档列表
     * @param knowledgeDomain 知识域（可选）
     * @param status 状态（可选）
     * @return 文档列表
     */
    List<Document> getDocumentList(String knowledgeDomain, Integer status);

    /**
     * 根据用户ID获取文档列表
     * @param userId 用户ID
     * @return 文档列表
     */
    List<Document> getDocumentsByUserId(Long userId);

    /**
     * 根据ID获取文档
     * @param id 文档ID
     * @return 文档信息
     */
    Document getDocumentById(Long id);

    /**
     * 删除文档
     * @param id 文档ID
     */
    void deleteDocument(Long id);

    /**
     * 更新文档
     * @param document 文档信息
     */
    void updateDocument(Document document);

    /**
     * 处理文档（解析、切片、向量化）
     * @param documentId 文档ID
     */
    void processDocument(Long documentId) throws Exception;

    /**
     * 搜索文档
     * @param keyword 关键词
     * @return 文档列表
     */
    List<Document> searchDocuments(String keyword);

    /**
     * 批量删除文档
     * @param ids 文档ID列表
     */
    void batchDeleteDocuments(List<Long> ids);
}