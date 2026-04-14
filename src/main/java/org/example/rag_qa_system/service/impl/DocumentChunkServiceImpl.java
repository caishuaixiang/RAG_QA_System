package org.example.rag_qa_system.service.impl;

import org.example.rag_qa_system.entity.DocumentChunk;
import org.example.rag_qa_system.mapper.DocumentChunkMapper;
import org.example.rag_qa_system.service.DocumentChunkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档切片服务实现类
 */
@Service
public class DocumentChunkServiceImpl implements DocumentChunkService {

    @Autowired
    private DocumentChunkMapper documentChunkMapper;

    @Override
    public void createChunks(Long documentId, List<String> chunks) {
        // 清除现有切片
        deleteChunksByDocumentId(documentId);

        // 创建新切片
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(documentId);
            chunk.setChunkContent(chunks.get(i));
            chunk.setChunkIndex(i);
            chunk.setStatus(0); // 未处理
            chunk.setCreateTime(LocalDateTime.now());
            documentChunkMapper.insert(chunk);
        }
    }

    @Override
    public List<DocumentChunk> getChunksByDocumentId(Long documentId) {
        return documentChunkMapper.findByDocumentId(documentId);
    }

    @Override
    public List<DocumentChunk> getChunksByDocumentIdAndTags(Long documentId, String tags) {
        return documentChunkMapper.findByDocumentIdAndTags(documentId, tags);
    }

    @Override
    public void batchSaveChunks(List<DocumentChunk> chunks) {
        documentChunkMapper.batchInsert(chunks);
    }

    @Override
    public void deleteChunksByDocumentId(Long documentId) {
        documentChunkMapper.deleteByDocumentId(documentId);
    }

    @Override
    public void deleteChunkById(Long chunkId) {
        documentChunkMapper.delete(chunkId);
    }

    @Override
    public void updateDocument(DocumentChunk chunk) {
        documentChunkMapper.update(chunk);
    }
}