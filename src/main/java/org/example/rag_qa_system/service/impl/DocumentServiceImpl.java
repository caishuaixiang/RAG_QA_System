package org.example.rag_qa_system.service.impl;

import org.example.rag_qa_system.entity.Document;
import org.example.rag_qa_system.entity.DocumentChunk;
import org.example.rag_qa_system.mapper.DocumentMapper;
import org.example.rag_qa_system.service.DocumentService;
import org.example.rag_qa_system.service.DocumentChunkService;
import org.example.rag_qa_system.service.VectorDatabaseService;
import org.example.rag_qa_system.utils.DocumentParser;
import org.example.rag_qa_system.utils.FileUtils;
import org.example.rag_qa_system.utils.TextChunker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档服务实现类
 */
@Service
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private DocumentParser documentParser;

    @Autowired
    @Qualifier("vectorDatabaseServiceImpl")
    private VectorDatabaseService vectorDatabaseService;

    @Autowired
    private DocumentChunkService documentChunkService;

    @Autowired
    private FileUtils fileUtils;

    @Override
    public Document uploadDocument(MultipartFile file, String knowledgeDomain) throws Exception {
        return uploadDocument(file, knowledgeDomain, null);
    }

    @Override
    public Document uploadDocument(MultipartFile file, String knowledgeDomain, Long userId) throws Exception {
        // 保存文件到本地
        String fileName = file.getOriginalFilename();
        String filePath = fileUtils.saveFile(file);

        // 创建文档对象
        Document document = new Document();
        document.setName(fileName);
        document.setOriginalName(fileName);
        document.setType(fileUtils.getFileType(fileName));
        document.setSize(file.getSize());
        document.setPath(filePath);
        document.setFilePath(filePath);
        document.setKnowledgeDomain(knowledgeDomain);
        document.setUserId(userId);  // 设置用户ID
        document.setStatus(0); // 未处理
        document.setCreateTime(LocalDateTime.now());
        document.setUpdateTime(LocalDateTime.now());

        // 保存文档信息到数据库
        documentMapper.insert(document);

        return document;
    }

    @Override
    public List<Document> getDocumentList(String knowledgeDomain, Integer status) {
        if (knowledgeDomain != null && !knowledgeDomain.isEmpty()) {
            return documentMapper.findByKnowledgeDomain(knowledgeDomain);
        } else if (status != null) {
            return documentMapper.findByStatus(status);
        } else {
            return documentMapper.findAll();
        }
    }

    @Override
    public List<Document> getDocumentsByUserId(Long userId) {
        return documentMapper.findByUserId(userId);
    }

    @Override
    public Document getDocumentById(Long id) {
        return documentMapper.findById(id);
    }

    @Override
    public void deleteDocument(Long id) {
        // 删除文档文件
        Document document = documentMapper.findById(id);
        if (document != null && document.getFilePath() != null) {
            File file = new File(document.getFilePath());
            if (file.exists()) {
                file.delete();
            }
        }

        // 删除文档切片
        documentChunkService.deleteChunksByDocumentId(id);

        // 删除向量数据库中的数据
        vectorDatabaseService.deleteChunksFromVectorDB(id);

        // 删除文档记录
        documentMapper.delete(id);
    }

    @Override
    public void updateDocument(Document document) {
        document.setUpdateTime(LocalDateTime.now());
        documentMapper.update(document);
    }

    @Override
    public void processDocument(Long documentId) throws Exception {
        Document document = documentMapper.findById(documentId);
        if (document == null) {
            throw new Exception("文档不存在");
        }

        String content;
        List<DocumentParser.PagePosition> pagePositions = null;

        // 解析文档内容，PDF文件提取页码信息
        if (document.getFilePath() != null && document.getFilePath().toLowerCase().endsWith(".pdf")) {
            DocumentParser.ParseResult parseResult = documentParser.parsePdfWithPageInfo(document.getFilePath());
            content = parseResult.getContent();
            pagePositions = parseResult.getPagePositions();
            System.out.println("PDF parsed with " + (pagePositions != null ? pagePositions.size() : 0) + " pages");
        } else {
            content = documentParser.parseDocument(document.getFilePath());
        }

        document.setContent(content);
        document.setStatus(1); // 已处理
        document.setUpdateTime(LocalDateTime.now());
        documentMapper.update(document);

        // 使用增强版智能切片算法（段落感知 + 重叠窗口）
        List<TextChunker.ChunkResult> chunkResults = TextChunker.smartSplitWithOverlap(content, 500, 50);

        // 提取章节信息
        List<TextChunker.SectionInfo> sections = TextChunker.extractSections(content);

        // 创建文档切片（包含位置溯源信息和页码）
        documentChunkService.createChunksWithLocation(documentId, chunkResults, sections, pagePositions);

        // 向量化
        List<DocumentChunk> documentChunks = documentChunkService.getChunksByDocumentId(documentId);
        System.out.println("Processing document " + documentId + ":" + chunkResults.size() + " chunks created");
        vectorDatabaseService.addChunksToVectorDB(documentId, documentChunks);
    }

    @Override
    public List<Document> searchDocuments(String keyword) {
        return documentMapper.findByCondition(null, null, null, keyword);
    }

    @Override
    public void batchDeleteDocuments(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            for (Long id : ids) {
                deleteDocument(id);
            }
        }
    }
}