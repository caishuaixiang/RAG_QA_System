package org.example.rag_qa_system.service.impl;

import org.example.rag_qa_system.entity.Document;
import org.example.rag_qa_system.entity.KnowledgeBase;
import org.example.rag_qa_system.entity.DocumentChunk;
import org.example.rag_qa_system.mapper.KnowledgeBaseMapper;
import org.example.rag_qa_system.mapper.DocumentMapper;
import org.example.rag_qa_system.mapper.DocumentChunkMapper;
import org.example.rag_qa_system.service.KnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库服务实现类
 */
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private DocumentChunkMapper documentChunkMapper;

    @Override
    public KnowledgeBase createKnowledgeBase(KnowledgeBase knowledgeBase) {
        knowledgeBase.setCreateTime(LocalDateTime.now());
        knowledgeBase.setUpdateTime(LocalDateTime.now());
        knowledgeBase.setDocumentCount(0);
        if (knowledgeBase.getStatus() == null) {
            knowledgeBase.setStatus(1); // 默认启用
        }
        if (knowledgeBase.getIsPublic() == null) {
            knowledgeBase.setIsPublic(0); // 默认私有
        }
        knowledgeBaseMapper.insert(knowledgeBase);
        return knowledgeBase;
    }

    @Override
    public KnowledgeBase getKnowledgeBaseById(Long id) {
        return knowledgeBaseMapper.findById(id);
    }

    @Override
    public List<KnowledgeBase> getAllKnowledgeBases() {
        return knowledgeBaseMapper.findAll();
    }

    @Override
    public List<KnowledgeBase> getKnowledgeBasesByUserId(Long userId) {
        return knowledgeBaseMapper.findByUserId(userId);
    }

    @Override
    public List<KnowledgeBase> searchKnowledgeBases(Long userId, String name, Integer status) {
        return knowledgeBaseMapper.findByCondition(userId, name, status, null);
    }

    @Override
    public KnowledgeBase updateKnowledgeBase(KnowledgeBase knowledgeBase) {
        knowledgeBase.setUpdateTime(LocalDateTime.now());
        knowledgeBaseMapper.update(knowledgeBase);
        return knowledgeBaseMapper.findById(knowledgeBase.getId());
    }

    @Override
    @Transactional
    public void deleteKnowledgeBase(Long id) {
        // 删除知识库下的所有文档和切片
        List<Document> documents = documentMapper.findByKnowledgeDomain(String.valueOf(id));
        for (Document document : documents) {
            documentChunkMapper.deleteByDocumentId(document.getId());
            documentMapper.delete(document.getId());
        }
        // 删除知识库
        knowledgeBaseMapper.delete(id);
    }

    @Override
    public void updateDocumentCount(Long id) {
        List<Document> documents = documentMapper.findByKnowledgeDomain(String.valueOf(id));
        int count = documents.size();
        knowledgeBaseMapper.updateDocumentCount(id, count, LocalDateTime.now());
    }

    @Override
    public KnowledgeBaseStats getKnowledgeBaseStats(Long id) {
        KnowledgeBaseStats stats = new KnowledgeBaseStats();

        // 获取文档数量
        List<Document> documents = documentMapper.findByKnowledgeDomain(String.valueOf(id));
        stats.setDocumentCount(documents.size());

        // 计算总大小和切片数量
        long totalSize = 0;
        int chunkCount = 0;
        for (Document document : documents) {
            if (document.getSize() != null) {
                totalSize += document.getSize();
            }
            List<DocumentChunk> chunks = documentChunkMapper.findByDocumentId(document.getId());
            chunkCount += chunks.size();
        }
        stats.setChunkCount(chunkCount);
        stats.setTotalSize(formatSize(totalSize));

        // 问答次数暂时设为0，后续可以实现
        stats.setQaCount(0);

        return stats;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}