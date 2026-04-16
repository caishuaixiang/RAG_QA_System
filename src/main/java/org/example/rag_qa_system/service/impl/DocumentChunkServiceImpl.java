package org.example.rag_qa_system.service.impl;

import org.example.rag_qa_system.entity.DocumentChunk;
import org.example.rag_qa_system.mapper.DocumentChunkMapper;
import org.example.rag_qa_system.service.DocumentChunkService;
import org.example.rag_qa_system.utils.TextChunker;
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
    public void createChunksWithLocation(Long documentId, List<TextChunker.ChunkResult> chunkResults,
                                         List<TextChunker.SectionInfo> sections) {
        // 清除现有切片
        deleteChunksByDocumentId(documentId);

        // 创建新切片（包含位置信息）
        for (int i = 0; i < chunkResults.size(); i++) {
            TextChunker.ChunkResult result = chunkResults.get(i);

            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(documentId);
            chunk.setChunkContent(result.getContent());
            chunk.setChunkIndex(i);
            chunk.setStatus(0); // 未处理

            // 设置位置溯源信息
            chunk.setStartPosition(result.getStartIndex());
            chunk.setEndPosition(result.getEndIndex());
            chunk.setParagraphIndex(result.getParagraphIndex());
            chunk.setLineRange(result.getLineRange());

            // 根据字符位置查找所属章节
            if (sections != null && !sections.isEmpty()) {
                String sectionTitle = findSectionTitle(result.getStartIndex(), sections);
                chunk.setSectionTitle(sectionTitle);
            }

            chunk.setCreateTime(LocalDateTime.now());
            documentChunkMapper.insert(chunk);
        }
    }

    /**
     * 根据字符位置查找所属章节标题
     */
    private String findSectionTitle(int charIndex, List<TextChunker.SectionInfo> sections) {
        String currentSection = null;
        for (TextChunker.SectionInfo section : sections) {
            if (section.getStartIndex() <= charIndex) {
                currentSection = section.getTitle();
            } else {
                break;
            }
        }
        return currentSection;
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