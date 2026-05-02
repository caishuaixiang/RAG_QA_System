package org.example.rag_qa_system.service.impl;

import org.example.rag_qa_system.entity.DocumentChunk;
import org.example.rag_qa_system.mapper.DocumentChunkMapper;
import org.example.rag_qa_system.service.DocumentChunkService;
import org.example.rag_qa_system.utils.DocumentParser;
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
        createChunksWithLocation(documentId, chunkResults, sections, null);
    }

    @Override
    public void createChunksWithLocation(Long documentId, List<TextChunker.ChunkResult> chunkResults,
                                         List<TextChunker.SectionInfo> sections,
                                         List<DocumentParser.PagePosition> pagePositions) {
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

            // 设置页码（如果有）
            if (pagePositions != null && !pagePositions.isEmpty()) {
                Integer pageNumber = DocumentParser.getPageNumber(result.getStartIndex(), pagePositions);
                chunk.setPageNumber(pageNumber);
            }

            // 优先从切片内容中提取标题
            String contentTitle = extractTitleFromContent(result.getContent());
            if (contentTitle != null && !contentTitle.isEmpty()) {
                chunk.setSectionTitle(contentTitle);
            } else if (sections != null && !sections.isEmpty()) {
                // 如果内容中没有标题，则根据字符位置查找所属章节
                String sectionTitle = findSectionTitle(result.getStartIndex(), sections);
                chunk.setSectionTitle(sectionTitle);
            }

            chunk.setCreateTime(LocalDateTime.now());
            documentChunkMapper.insert(chunk);
        }
    }

    /**
     * 根据字符位置查找所属章节标题
     * 优先级：1. 从切片内容中提取的标题 2. 切片位置之前的章节标题
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

    /**
     * 从切片内容中提取有效标题
     * 优先级高于章节标题
     */
    private String extractTitleFromContent(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        String[] lines = content.split("\\r?\\n");
        String lastPotentialTitle = null;

        for (int i = 0; i < lines.length && i < 10; i++) {  // 只检查前10行
            String line = lines[i].trim();

            // 跳过空行和纯数字（页码）
            if (line.isEmpty() || line.matches("^\\d+$")) {
                continue;
            }

            // 跳过太短或太长的行
            if (line.length() < 5 || line.length() > 50) {
                continue;
            }

            // 不以标点符号结尾（除顿号、书名号）
            char lastChar = line.charAt(line.length() - 1);
            if ("。！？，；：、.!?,:;".indexOf(lastChar) >= 0) {
                continue;
            }

            // 检查是否是手册名称格式（如"西安科技大学研究生请假制度"）
            if (line.matches(".*[大学学院学校].*[制度规定办法条例守则手册指南].*")) {
                return line;
            }

            // 检查是否包含关键词结尾
            String[] endKeywords = {"制度", "规定", "办法", "条例", "守则", "手册", "指南", "须知"};
            for (String keyword : endKeywords) {
                if (line.endsWith(keyword)) {
                    // 记录这个潜在标题，但继续查找更好的
                    if (lastPotentialTitle == null || line.contains("大学") || line.contains("学院")) {
                        lastPotentialTitle = line;
                    }
                }
            }

            // 检查是否是章节标题格式
            if (line.matches("^第[一二三四五六七八九十百千万零\\d]+[章节篇部].*") ||
                    line.matches("^[一二三四五六七八九十]+、.*") ||
                    line.matches("^\\d+[\\.、．].*")) {
                return line;
            }
        }

        return lastPotentialTitle;
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