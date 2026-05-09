package org.example.rag_qa_system.service;

import org.example.rag_qa_system.dto.Bm25Doc;
import org.example.rag_qa_system.dto.HybridDoc;
import org.example.rag_qa_system.dto.SearchResult;
import org.example.rag_qa_system.entity.DocumentChunk;
import org.example.rag_qa_system.mapper.DocumentChunkMapper;
import org.example.rag_qa_system.utils.VectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合检索服务 - 融合BM25和向量检索结果（RRF算法）
 */
@Service
public class HybridSearchService {

    private static final Logger logger = LoggerFactory.getLogger(HybridSearchService.class);

    /**
     * RRF常数K，控制排名靠后的文档权重衰减速度
     */
    private static final int RRF_K = 60;

    @Autowired
    private BM25SearchService bm25SearchService;

    @Autowired
    private VectorUtils vectorUtils;

    @Autowired
    @Qualifier("vectorDatabaseServiceImpl")
    private VectorDatabaseService vectorDatabaseService;

    @Autowired
    private DocumentChunkMapper documentChunkMapper;

    /**
     * 混合检索：融合BM25和向量检索结果
     * @param query 查询文本
     * @param initialK 每路检索的候选数量
     * @param finalK 最终返回的文档数量
     * @return 融合后的文档列表（按RRF分数降序）
     */
    public List<HybridDoc> hybridSearch(String query, int initialK, int finalK) {
        return hybridSearch(query, initialK, finalK, null);
    }

    /**
     * 混合检索：融合BM25和向量检索结果（支持知识库隔离）
     * @param query 查询文本
     * @param initialK 每路检索的候选数量
     * @param finalK 最终返回的文档数量
     * @param knowledgeBaseId 知识库ID（为null时查询所有知识库）
     * @return 融合后的文档列表（按RRF分数降序）
     */
    public List<HybridDoc> hybridSearch(String query, int initialK, int finalK, Long knowledgeBaseId) {
        logger.info("混合检索开始，query='{}', initialK={}, finalK={}, knowledgeBaseId={}", query, initialK, finalK, knowledgeBaseId);

        // 获取知识库过滤用的文档ID集合
        Set<Long> allowedDocumentIds = null;
        if (knowledgeBaseId != null) {
            try {
                List<Long> docIds = documentChunkMapper.findDocumentIdsByKnowledgeBaseId(String.valueOf(knowledgeBaseId));
                allowedDocumentIds = new HashSet<>(docIds);
                logger.info("知识库 {} 包含 {} 个文档", knowledgeBaseId, allowedDocumentIds.size());
            } catch (Exception e) {
                logger.error("获取知识库文档ID失败: ", e);
            }
        }

        // 1. 执行BM25检索（获取完整DocumentChunk，支持知识库过滤）
        List<DocumentChunk> bm25Chunks = Collections.emptyList();
        try {
            bm25Chunks = bm25SearchService.bm25SearchWithChunks(query, initialK, documentChunkMapper, allowedDocumentIds);
            logger.info("BM25检索完成，返回 {} 条结果", bm25Chunks.size());
        } catch (Exception e) {
            logger.error("BM25检索异常: ", e);
        }

        // 2. 执行向量检索（带距离，支持知识库过滤）
        List<SearchResult> vectorResults = Collections.emptyList();
        try {
            float[] queryVector = vectorUtils.getVector(query);
            vectorResults = vectorDatabaseService.searchSimilarChunksWithDistance(queryVector, initialK, knowledgeBaseId);
            logger.info("向量检索完成，返回 {} 条结果", vectorResults.size());
        } catch (Exception e) {
            logger.error("向量检索异常: ", e);
        }

        // 3. 计算RRF分数并融合
        List<HybridDoc> hybridDocs = computeRrfScores(bm25Chunks, vectorResults);

        // 4. 按RRF分数降序排序，取前finalK个
        List<HybridDoc> finalResults = hybridDocs.stream()
                .sorted(Comparator.comparingDouble(HybridDoc::getRrfScore).reversed())
                .limit(finalK)
                .collect(Collectors.toList());

        logger.info("混合检索完成，融合后返回 {} 条结果", finalResults.size());
        return finalResults;
    }

    /**
     * 计算RRF（Reciprocal Rank Fusion）分数
     * score = sum(1 / (K + rank))，K=60
     */
    private List<HybridDoc> computeRrfScores(List<DocumentChunk> bm25Chunks,
                                              List<SearchResult> vectorResults) {
        // 使用LinkedHashMap保持插入顺序，key=docId(chunk的数据库ID)
        Map<Long, HybridDoc> docMap = new LinkedHashMap<>();
        // 记录每个文档在各路检索中的排名
        Map<Long, Map<String, Integer>> rankMaps = new HashMap<>();

        // 处理BM25结果，构建排名
        for (int i = 0; i < bm25Chunks.size(); i++) {
            DocumentChunk chunk = bm25Chunks.get(i);
            Long chunkId = chunk.getId();
            if (chunkId == null) continue;
            int rank = i + 1;

            HybridDoc hd = new HybridDoc();
            hd.setDocId(String.valueOf(chunkId));
            hd.setContent(chunk.getChunkContent());
            hd.setSourceDocId(chunk.getDocumentId());
            hd.setVectorSimilarity(0.0); // BM25没有向量相似度
            hd.setVectorHit(false); // 初始为false，后续向量检索如果命中会设为true
            docMap.put(chunkId, hd);

            rankMaps.computeIfAbsent(chunkId, k -> new HashMap<>()).put("bm25", rank);
        }

        // 处理向量检索结果，构建排名并获取相似度
        for (int i = 0; i < vectorResults.size(); i++) {
            SearchResult searchResult = vectorResults.get(i);
            DocumentChunk chunk = searchResult.getChunk();
            Long chunkId = chunk.getId();
            if (chunkId == null) continue;
            int rank = i + 1;

            // 从SearchResult获取相似度百分比
            double vectorSimilarity = searchResult.getSimilarityPercentage();

            if (docMap.containsKey(chunkId)) {
                // 已存在，更新向量相似度和命中标记
                HybridDoc existingDoc = docMap.get(chunkId);
                existingDoc.setVectorSimilarity(vectorSimilarity);
                existingDoc.setVectorHit(true);
            } else {
                // 不存在，创建新的HybridDoc
                HybridDoc hd = new HybridDoc();
                hd.setDocId(String.valueOf(chunkId));
                hd.setContent(chunk.getChunkContent());
                hd.setSourceDocId(chunk.getDocumentId());
                hd.setVectorSimilarity(vectorSimilarity);
                hd.setVectorHit(true);
                docMap.put(chunkId, hd);
            }

            rankMaps.computeIfAbsent(chunkId, k -> new HashMap<>()).put("vector", rank);
        }

        // 计算每个文档的RRF分数
        for (Map.Entry<Long, HybridDoc> entry : docMap.entrySet()) {
            Long chunkId = entry.getKey();
            HybridDoc hd = entry.getValue();
            Map<String, Integer> ranks = rankMaps.getOrDefault(chunkId, Collections.emptyMap());

            double rrfScore = 0.0;
            // BM25排名贡献
            if (ranks.containsKey("bm25")) {
                rrfScore += 1.0 / (RRF_K + ranks.get("bm25"));
            }
            // 向量排名贡献
            if (ranks.containsKey("vector")) {
                rrfScore += 1.0 / (RRF_K + ranks.get("vector"));
            }

            hd.setRrfScore(rrfScore);
        }

        return new ArrayList<>(docMap.values());
    }
}
