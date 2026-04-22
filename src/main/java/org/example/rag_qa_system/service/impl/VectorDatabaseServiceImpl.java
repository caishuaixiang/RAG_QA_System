package org.example.rag_qa_system.service.impl;

import org.example.rag_qa_system.entity.DocumentChunk;
import org.example.rag_qa_system.service.DocumentChunkService;
import org.example.rag_qa_system.service.VectorDatabaseService;
import org.example.rag_qa_system.utils.VectorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 向量数据库服务实现类 (ChromaDB)
 */
@Service("vectorDatabaseServiceImpl")
public class VectorDatabaseServiceImpl implements VectorDatabaseService {

    @Autowired
    private VectorUtils vectorUtils;

    @Autowired
    private DocumentChunkService documentChunkService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${vector.db.url}")
    private String vectorDbUrl;

    @Value("${vector.db.collection}")
    private String vectorDbCollection;

    @Value("${vector.db.token:}")
    private String vectorDbToken;

    // 缓存 collection ID
    private String collectionId = null;

    /**
     * 创建带认证的请求头
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (vectorDbToken != null && !vectorDbToken.isEmpty()) {
            headers.set("Authorization", "Bearer " + vectorDbToken);
            headers.set("X-Chroma-Token", vectorDbToken);
        }
        return headers;
    }

    /**
     * 获取或创建 collection
     */
    private String getOrCreateCollectionId() {
        if (collectionId != null) {
            return collectionId;
        }

        // 先尝试获取已存在的 collection
        try {
            String getUrl = vectorDbUrl + "/api/v1/collections/" + vectorDbCollection;
            HttpEntity<Void> requestEntity = new HttpEntity<>(createHeaders());
            var responseEntity = restTemplate.exchange(
                    getUrl,
                    org.springframework.http.HttpMethod.GET,
                    requestEntity,
                    Map.class
            );
            Map<String, Object> existingCollection = responseEntity.getBody();

            if (existingCollection != null && existingCollection.containsKey("id")) {
                collectionId = (String) existingCollection.get("id");
                return collectionId;
            }
        } catch (Exception e) {
            // Collection not found, will try to create
        }

        // 创建新的 collection
        try {
            String createUrl = vectorDbUrl + "/api/v1/collections";
            Map<String, Object> request = new HashMap<>();
            request.put("name", vectorDbCollection);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, createHeaders());

            Map<String, Object> response = restTemplate.postForObject(
                    createUrl,
                    requestEntity,
                    Map.class
            );

            if (response != null && response.containsKey("id")) {
                collectionId = (String) response.get("id");
                return collectionId;
            }
        } catch (Exception e) {
            System.err.println("Failed to create ChromaDB collection: " + e.getMessage());
        }

        return null;
    }

    @Override
    public void addChunksToVectorDB(Long documentId, List<DocumentChunk> chunks) {
        String collId = getOrCreateCollectionId();
        if (collId == null) {
            System.err.println("Failed to get collection ID");
            return;
        }

        List<String> ids = new ArrayList<>();
        List<float[]> embeddings = new ArrayList<>();
        List<Map<String, Object>> metadatas = new ArrayList<>();
        List<String> documents = new ArrayList<>();

        for (DocumentChunk chunk : chunks) {
            // 获取向量
            float[] vector = vectorUtils.getVector(chunk.getChunkContent());

            ids.add(documentId + "_" + chunk.getChunkIndex());
            embeddings.add(vector);
            documents.add(chunk.getChunkContent());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("document_id", documentId);
            metadata.put("chunk_index", chunk.getChunkIndex());
            metadatas.add(metadata);

            // 更新切片状态
            chunk.setStatus(1);
            documentChunkService.updateDocument(chunk);
        }

        Map<String, Object> request = new HashMap<>();
        request.put("ids", ids);
        request.put("embeddings", embeddings);
        request.put("metadatas", metadatas);
        request.put("documents", documents);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, createHeaders());

        try {
            // ChromaDB add 返回 Boolean，不是 Map
            Boolean result = restTemplate.postForObject(
                    vectorDbUrl + "/api/v1/collections/" + collId + "/add",
                    requestEntity,
                    Boolean.class
            );
            if (Boolean.TRUE.equals(result)) {
                System.out.println("Successfully added " + ids.size() + " chunks to ChromaDB");
            } else {
                System.err.println("ChromaDB add returned false or null");
            }
        } catch (Exception e) {
            System.err.println("Failed to add chunks to ChromaDB: " + e.getMessage());
        }
    }

    @Override
    public List<DocumentChunk> searchSimilarChunks(float[] queryVector, int topK) {
        String collId = getOrCreateCollectionId();
        if (collId == null) {
            System.err.println("Failed to get collection ID");
            return new ArrayList<>();
        }
        System.out.println("Searching in collection: " + collId + " with topK: " + topK);

        // ChromaDB query 格式
        Map<String, Object> request = new HashMap<>();
        request.put("query_embeddings", Arrays.asList(queryVector));
        request.put("n_results", topK);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, createHeaders());

        try {
            Map<String, Object> response = restTemplate.postForObject(
                    vectorDbUrl + "/api/v1/collections/" + collId + "/query",
                    requestEntity,
                    Map.class
            );
            System.out.println("ChromaDB query response: " + response);

            List<DocumentChunk> result = new ArrayList<>();

            if (response != null) {
                // ChromaDB 返回格式: {ids: [[...]], documents: [[...]], metadatas: [[...]], distances: [[...]]}
                List<List<String>> ids = (List<List<String>>) response.get("ids");
                List<List<String>> documents = (List<List<String>>) response.get("documents");
                List<List<Map<String, Object>>> metadatas = (List<List<Map<String, Object>>>) response.get("metadatas");

                if (ids != null && !ids.isEmpty() && !ids.get(0).isEmpty()) {
                    List<String> idList = ids.get(0);
                    List<String> docList = documents != null && !documents.isEmpty() ? documents.get(0) : new ArrayList<>();
                    List<Map<String, Object>> metaList = metadatas != null && !metadatas.isEmpty() ? metadatas.get(0) : new ArrayList<>();

                    for (int i = 0; i < idList.size(); i++) {
                        DocumentChunk chunk = new DocumentChunk();
                        chunk.setChunkContent(docList.size() > i ? docList.get(i) : "");

                        if (metaList.size() > i) {
                            Map<String, Object> meta = metaList.get(i);
                            if (meta.get("document_id") != null) {
                                chunk.setDocumentId(((Number) meta.get("document_id")).longValue());
                            }
                            if (meta.get("chunk_index") != null) {
                                chunk.setChunkIndex(((Number) meta.get("chunk_index")).intValue());
                            }
                        }

                        result.add(chunk);
                    }
                }
            }

            return result;

        } catch (Exception e) {
            System.err.println("Failed to search ChromaDB: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    @Override
    public void deleteChunksFromVectorDB(Long documentId) {
        String collId = getOrCreateCollectionId();
        if (collId == null) {
            return;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("where", Map.of("document_id", documentId));

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, createHeaders());

        try {
            restTemplate.postForObject(
                    vectorDbUrl + "/api/v1/collections/" + collId + "/delete",
                    requestEntity,
                    Object.class
            );
        } catch (Exception e) {
            System.err.println("Failed to delete chunks: " + e.getMessage());
        }
    }
}