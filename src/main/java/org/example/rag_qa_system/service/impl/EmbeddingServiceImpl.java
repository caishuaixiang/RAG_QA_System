package org.example.rag_qa_system.service.impl;

import org.example.rag_qa_system.service.EmbeddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Embedding服务实现类
 */
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${embedding.api.url}")
    private String embeddingApiUrl;

    @Value("${embedding.api.key}")
    private String embeddingApiKey;

    @Value("${embedding.api.model:BAAI/bge-large-zh-v1.5}")
    private String embeddingModel;

    @Override
    public String generateEmbedding(String text) {
        // 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + embeddingApiKey);

        // 准备请求体（OpenAI 兼容格式）
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", embeddingModel);
        requestBody.put("input", text);

        // 发送请求
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(embeddingApiUrl, requestEntity, Map.class);

        // 解析响应
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> responseBody = response.getBody();
            Object data = responseBody.get("data");
            if (data instanceof List) {
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) data;
                if (!dataList.isEmpty()) {
                    Map<String, Object> firstItem = dataList.get(0);
                    Object embedding = firstItem.get("embedding");
                    return embedding != null ? embedding.toString() : "[]";
                }
            }
        }

        return "[]";
    }
}