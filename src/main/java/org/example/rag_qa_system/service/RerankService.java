package org.example.rag_qa_system.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 重排序服务 - 调用硅基流动Rerank API
 * 使用BAAI/bge-reranker-v2-m3模型对候选文档进行重排序
 */
@Service
public class RerankService {

    private static final Logger logger = LoggerFactory.getLogger(RerankService.class);

    @Value("${api-key}")
    private String apiKey;

    @Value("${siliconflow.rerank.url}")
    private String rerankUrl;

    @Value("${siliconflow.rerank.model}")
    private String rerankModel;

    @Value("${siliconflow.rerank.top-n:5}")
    private int topN;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 重排序文档列表
     * @param query 查询文本
     * @param documentContents 候选文档内容列表
     * @return 重排序后的文档内容列表
     */
    public List<String> rerankDocuments(String query, List<String> documentContents) {
        if (documentContents == null || documentContents.isEmpty()) {
            logger.warn("rerankDocuments: 候选文档列表为空");
            return Collections.emptyList();
        }

        if (query == null || query.trim().isEmpty()) {
            logger.warn("rerankDocuments: 查询文本为空");
            return documentContents;
        }

        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", rerankModel);
            requestBody.put("query", query);
            requestBody.put("documents", documentContents);
            requestBody.put("top_n", Math.min(topN, documentContents.size()));

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            logger.debug("调用Rerank API: url={}, model={}, documents={}", rerankUrl, rerankModel, documentContents.size());

            // 发送POST请求
            Map<String, Object> response = restTemplate.postForObject(
                    rerankUrl,
                    requestEntity,
                    Map.class
            );

            // 解析响应
            return parseRerankResponse(response, documentContents);

        } catch (Exception e) {
            logger.error("Rerank API调用失败，降级返回原始顺序", e);
            // 降级：返回原始顺序，截取topN个
            return documentContents.subList(0, Math.min(topN, documentContents.size()));
        }
    }

    /**
     * 重排序文档列表（带分数版本）
     * @param query 查询文本
     * @param documentContents 候选文档内容列表
     * @return 重排序后的文档内容和分数列表
     */
    public List<Map<String, Object>> rerankDocumentsWithScore(String query, List<String> documentContents) {
        if (documentContents == null || documentContents.isEmpty()) {
            logger.warn("rerankDocumentsWithScore: 候选文档列表为空");
            return Collections.emptyList();
        }

        if (query == null || query.trim().isEmpty()) {
            logger.warn("rerankDocumentsWithScore: 查询文本为空");
            return Collections.emptyList();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", rerankModel);
            requestBody.put("query", query);
            requestBody.put("documents", documentContents);
            requestBody.put("top_n", Math.min(topN, documentContents.size()));

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            Map<String, Object> response = restTemplate.postForObject(
                    rerankUrl,
                    requestEntity,
                    Map.class
            );

            return parseRerankResponseWithScore(response, documentContents);

        } catch (Exception e) {
            logger.error("Rerank API调用失败，降级返回原始顺序", e);
            // 降级返回
            List<Map<String, Object>> fallback = new ArrayList<>();
            int limit = Math.min(topN, documentContents.size());
            for (int i = 0; i < limit; i++) {
                Map<String, Object> item = new HashMap<>();
                item.put("content", documentContents.get(i));
                item.put("index", i);
                item.put("relevance_score", 0.0);
                fallback.add(item);
            }
            return fallback;
        }
    }

    /**
     * 解析Rerank API响应
     * 响应格式: {"results": [{"index": 0, "relevance_score": 0.95}, ...]}
     */
    @SuppressWarnings("unchecked")
    private List<String> parseRerankResponse(Map<String, Object> response, List<String> originalContents) {
        if (response == null || !response.containsKey("results")) {
            logger.warn("Rerank API响应格式异常，返回原始顺序");
            return originalContents.subList(0, Math.min(topN, originalContents.size()));
        }

        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
        if (results == null || results.isEmpty()) {
            logger.warn("Rerank API返回空结果，返回原始顺序");
            return originalContents.subList(0, Math.min(topN, originalContents.size()));
        }

        // 按index重排文档
        List<String> rerankedContents = new ArrayList<>();
        for (Map<String, Object> result : results) {
            int index = ((Number) result.get("index")).intValue();
            double score = result.get("relevance_score") != null ?
                    ((Number) result.get("relevance_score")).doubleValue() : 0.0;

            if (index >= 0 && index < originalContents.size()) {
                rerankedContents.add(originalContents.get(index));
                logger.debug("Rerank: index={}, score={}", index, score);
            }
        }

        logger.info("Rerank完成，返回 {} 条结果", rerankedContents.size());
        return rerankedContents;
    }

    /**
     * 解析Rerank API响应（带分数版本）
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseRerankResponseWithScore(Map<String, Object> response,
                                                                     List<String> originalContents) {
        if (response == null || !response.containsKey("results")) {
            logger.warn("Rerank API响应格式异常，返回空结果");
            return Collections.emptyList();
        }

        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> rerankedResults = new ArrayList<>();
        for (Map<String, Object> result : results) {
            int index = ((Number) result.get("index")).intValue();
            double score = result.get("relevance_score") != null ?
                    ((Number) result.get("relevance_score")).doubleValue() : 0.0;

            if (index >= 0 && index < originalContents.size()) {
                Map<String, Object> item = new HashMap<>();
                item.put("content", originalContents.get(index));
                item.put("index", index);
                item.put("relevance_score", score);
                rerankedResults.add(item);
            }
        }

        logger.info("Rerank完成（带分数），返回 {} 条结果", rerankedResults.size());
        return rerankedResults;
    }
}
