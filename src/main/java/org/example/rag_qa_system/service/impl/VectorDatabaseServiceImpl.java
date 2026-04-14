package org.example.rag_qa_system.service.impl;

import org.example.rag_qa_system.entity.DocumentChunk;
import org.example.rag_qa_system.service.DocumentChunkService;
import org.example.rag_qa_system.service.VectorDatabaseService;
import org.example.rag_qa_system.utils.VectorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量数据库服务实现类
 */
@Service("vectorDatabaseServiceImpl")
public class VectorDatabaseServiceImpl implements VectorDatabaseService {

    @Autowired
    private VectorUtils vectorUtils;

    @Autowired
    private DocumentChunkService documentChunkService;

    @Value("${vector.db.url}")
    private String vectorDbUrl;

    @Value("${vector.db.collection}")
    private String vectorDbCollection;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void addChunksToVectorDB(Long documentId, List<DocumentChunk> chunks) {
        for (DocumentChunk chunk : chunks) {
            // 获取向量
            float[] vector = vectorUtils.getVector(chunk.getChunkContent());

            // 准备数据
            Map<String, Object> data = new HashMap<>();
            data.put("id", documentId + "_" + chunk.getChunkIndex());
            data.put("document_id", documentId);
            data.put("chunk_index", chunk.getChunkIndex());
            data.put("content", chunk.getChunkContent());
            data.put("vector", vector);

            // 添加到向量数据库
            restTemplate.postForObject(
                    vectorDbUrl + "/api/collections/" + vectorDbCollection + "/add",
                    data,
                    Map.class
            );

            // 更新切片状态
            chunk.setStatus(1); // 已处理
            documentChunkService.updateDocument(chunk);
        }
    }

    @Override
    public List<DocumentChunk> searchSimilarChunks(float[] queryVector, int topK) {
        Map<String, Object> request = new HashMap<>();
        request.put("vector", queryVector);
        request.put("top_k", topK);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                vectorDbUrl + "/api/collections/" + vectorDbCollection + "/search",
                request,
                Map.class
        );

        List<DocumentChunk> result = new ArrayList<>();
        if (response != null && response.containsKey("results")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            for (Map<String, Object> item : results) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocumentId(((Number) item.get("document_id")).longValue());
                chunk.setChunkIndex(((Number) item.get("chunk_index")).intValue());
                chunk.setChunkContent((String) item.get("content"));
                result.add(chunk);
            }
        }
        return result;
    }

    @Override
    public void deleteChunksFromVectorDB(Long documentId) {
        restTemplate.delete(
                vectorDbUrl + "/api/collections/" + vectorDbCollection + "/delete",
                documentId
        );
    }
}