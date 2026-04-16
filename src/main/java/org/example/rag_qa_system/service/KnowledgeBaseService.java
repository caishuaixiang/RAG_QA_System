package org.example.rag_qa_system.service;

import org.example.rag_qa_system.entity.KnowledgeBase;

import java.util.List;

/**
 * 知识库服务接口
 */
public interface KnowledgeBaseService {

    /**
     * 创建知识库
     * @param knowledgeBase 知识库信息
     * @return 创建后的知识库
     */
    KnowledgeBase createKnowledgeBase(KnowledgeBase knowledgeBase);

    /**
     * 根据ID获取知识库
     * @param id 知识库ID
     * @return 知识库信息
     */
    KnowledgeBase getKnowledgeBaseById(Long id);

    /**
     * 获取所有知识库
     * @return 知识库列表
     */
    List<KnowledgeBase> getAllKnowledgeBases();

    /**
     * 根据用户ID获取知识库
     * @param userId 用户ID
     * @return 知识库列表
     */
    List<KnowledgeBase> getKnowledgeBasesByUserId(Long userId);

    /**
     * 根据条件查询知识库
     * @param userId 用户ID
     * @param name 名称关键词
     * @param status 状态
     * @return 知识库列表
     */
    List<KnowledgeBase> searchKnowledgeBases(Long userId, String name, Integer status);

    /**
     * 更新知识库
     * @param knowledgeBase 知识库信息
     * @return 更新后的知识库
     */
    KnowledgeBase updateKnowledgeBase(KnowledgeBase knowledgeBase);

    /**
     * 删除知识库
     * @param id 知识库ID
     */
    void deleteKnowledgeBase(Long id);

    /**
     * 更新知识库文档数量
     * @param id 知识库ID
     */
    void updateDocumentCount(Long id);

    /**
     * 获取知识库统计信息
     * @param id 知识库ID
     * @return 统计信息
     */
    KnowledgeBaseStats getKnowledgeBaseStats(Long id);

    /**
     * 知识库统计信息类
     */
    class KnowledgeBaseStats {
        private Integer documentCount;
        private Integer chunkCount;
        private Integer qaCount;
        private String totalSize;

        public Integer getDocumentCount() { return documentCount; }
        public void setDocumentCount(Integer documentCount) { this.documentCount = documentCount; }
        public Integer getChunkCount() { return chunkCount; }
        public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
        public Integer getQaCount() { return qaCount; }
        public void setQaCount(Integer qaCount) { this.qaCount = qaCount; }
        public String getTotalSize() { return totalSize; }
        public void setTotalSize(String totalSize) { this.totalSize = totalSize; }
    }
}