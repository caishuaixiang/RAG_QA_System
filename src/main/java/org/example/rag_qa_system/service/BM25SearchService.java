package org.example.rag_qa_system.service;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.example.rag_qa_system.dto.Bm25Doc;
import org.example.rag_qa_system.entity.DocumentChunk;
import org.example.rag_qa_system.mapper.DocumentChunkMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * BM25检索服务 - 基于Lucene实现
 * 启动时从MySQL加载所有document_chunk记录建立内存索引
 */
@Service
public class BM25SearchService {

    private static final Logger logger = LoggerFactory.getLogger(BM25SearchService.class);

    @Autowired
    private DocumentChunkMapper documentChunkMapper;

    private RAMDirectory ramDirectory;
    private StandardAnalyzer analyzer;
    private boolean indexReady = false;

    /**
     * 应用启动后自动构建索引
     */
    @PostConstruct
    public void initIndex() {
        logger.info("BM25索引构建开始...");
        try {
            analyzer = new StandardAnalyzer();
            ramDirectory = new RAMDirectory();
            rebuildIndex();
            indexReady = true;
            logger.info("BM25索引构建完成，索引文档数: {}", getIndexCount());
        } catch (Exception e) {
            logger.error("BM25索引构建失败", e);
        }
    }

    /**
     * 重建索引（从MySQL加载所有切片）
     */
    public void rebuildIndex() {
        try {
            List<DocumentChunk> allChunks = documentChunkMapper.selectAll();
            logger.info("从MySQL加载了 {} 条切片记录", allChunks.size());

            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

            try (IndexWriter writer = new IndexWriter(ramDirectory, config)) {
                for (DocumentChunk chunk : allChunks) {
                    if (chunk.getChunkContent() == null || chunk.getChunkContent().isEmpty()) {
                        continue;
                    }
                    Document doc = new Document();
                    doc.add(new StringField("id", String.valueOf(chunk.getId()), Field.Store.YES));
                    doc.add(new StringField("documentId", String.valueOf(chunk.getDocumentId()), Field.Store.YES));
                    doc.add(new TextField("content", chunk.getChunkContent(), Field.Store.YES));
                    writer.addDocument(doc);
                }
                writer.commit();
            }
            logger.info("BM25索引重建完成");
        } catch (Exception e) {
            logger.error("BM25索引重建失败", e);
        }
    }

    /**
     * 获取索引中的文档总数
     */
    public int getIndexCount() {
        if (!indexReady) {
            return 0;
        }
        try (IndexReader reader = DirectoryReader.open(ramDirectory)) {
            return reader.numDocs();
        } catch (IOException e) {
            logger.error("获取索引文档数失败", e);
            return 0;
        }
    }

    /**
     * BM25检索
     * @param query 查询文本
     * @param topK 返回前K个结果
     * @return 检索结果列表
     */
    public List<Bm25Doc> bm25Search(String query, int topK) {
        List<Bm25Doc> results = new ArrayList<>();

        if (!indexReady) {
            logger.warn("BM25索引未就绪，跳过检索");
            return results;
        }

        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        try (IndexReader reader = DirectoryReader.open(ramDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            // 使用Lucene默认BM25相似度
            searcher.setSimilarity(new org.apache.lucene.search.similarities.BM25Similarity());

            QueryParser parser = new QueryParser("content", analyzer);
            // 对查询文本进行转义处理，防止特殊字符导致解析异常
            String escapedQuery = QueryParser.escape(query.trim());
            Query luceneQuery = parser.parse(escapedQuery);

            TopDocs topDocs = searcher.search(luceneQuery, topK);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;

            for (ScoreDoc scoreDoc : scoreDocs) {
                org.apache.lucene.document.Document doc = searcher.doc(scoreDoc.doc);
                Bm25Doc bm25Doc = new Bm25Doc();
                bm25Doc.setDocId(doc.get("id"));
                bm25Doc.setContent(doc.get("content"));
                bm25Doc.setBm25Score(scoreDoc.score);
                results.add(bm25Doc);
            }

            logger.debug("BM25检索完成，查询: '{}', 返回: {} 条结果", query, results.size());
        } catch (Exception e) {
            logger.error("BM25检索异常，查询: '{}'", query, e);
        }

        return results;
    }

    /**
     * BM25检索（返回完整DocumentChunk）
     * @param query 查询文本
     * @param topK 返回前K个结果
     * @param documentChunkMapper 用于查询完整DocumentChunk对象
     * @return DocumentChunk列表（带BM25分数）
     */
    public List<DocumentChunk> bm25SearchWithChunks(String query, int topK, DocumentChunkMapper mapper) {
        return bm25SearchWithChunks(query, topK, mapper, null);
    }

    /**
     * BM25检索（返回完整DocumentChunk，支持知识库过滤）
     * @param query 查询文本
     * @param topK 返回前K个结果
     * @param documentChunkMapper 用于查询完整DocumentChunk对象
     * @param allowedDocumentIds 允许的文档ID集合（为null时不过滤）
     * @return DocumentChunk列表（带BM25分数）
     */
    public List<DocumentChunk> bm25SearchWithChunks(String query, int topK, DocumentChunkMapper mapper, Set<Long> allowedDocumentIds) {
        List<DocumentChunk> results = new ArrayList<>();

        if (!indexReady) {
            logger.warn("BM25索引未就绪，跳过检索");
            return results;
        }

        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        // 如果需要过滤，搜索更多结果以确保有足够的过滤后结果
        int searchK = allowedDocumentIds != null ? topK * 3 : topK;

        try (IndexReader reader = DirectoryReader.open(ramDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(new org.apache.lucene.search.similarities.BM25Similarity());

            QueryParser parser = new QueryParser("content", analyzer);
            String escapedQuery = QueryParser.escape(query.trim());
            Query luceneQuery = parser.parse(escapedQuery);

            TopDocs topDocs = searcher.search(luceneQuery, searchK);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;

            for (ScoreDoc scoreDoc : scoreDocs) {
                if (results.size() >= topK) {
                    break;
                }
                org.apache.lucene.document.Document doc = searcher.doc(scoreDoc.doc);
                String idStr = doc.get("id");
                String docIdStr = doc.get("documentId");

                // 知识库过滤
                if (allowedDocumentIds != null && docIdStr != null) {
                    try {
                        Long documentId = Long.parseLong(docIdStr);
                        if (!allowedDocumentIds.contains(documentId)) {
                            continue; // 跳过不属于指定知识库的文档
                        }
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }

                try {
                    Long chunkId = Long.parseLong(idStr);
                    DocumentChunk chunk = mapper.findById(chunkId);
                    if (chunk != null) {
                        results.add(chunk);
                    }
                } catch (Exception e) {
                    logger.warn("无法解析chunk id: {}", idStr);
                }
            }

            logger.debug("BM25检索完成，查询: '{}', 返回: {} 条结果 (过滤: {})", query, results.size(), allowedDocumentIds != null);
        } catch (Exception e) {
            logger.error("BM25检索异常，查询: '{}'", query, e);
        }

        return results;
    }
}
