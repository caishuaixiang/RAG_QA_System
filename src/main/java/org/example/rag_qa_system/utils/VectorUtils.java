package org.example.rag_qa_system.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量工具类
 */
@Component
public class VectorUtils {

    @Value("${embedding.api.url}")
    private String embeddingApiUrl;

    @Value("${embedding.api.key}")
    private String embeddingApiKey;

    @Value("${embedding.api.model:BAAI/bge-large-zh-v1.5}")
    private String embeddingModel;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 获取文本向量
     * @param text 文本内容
     * @return 向量数组
     */
    public float[] getVector(String text) {
        try {
            // 准备请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + embeddingApiKey);

            // 准备请求体（OpenAI 兼容格式）
            Map<String, Object> request = new HashMap<>();
            request.put("input", text);
            request.put("model", embeddingModel);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, headers);

            Map<String, Object> response = restTemplate.postForObject(
                    embeddingApiUrl,
                    requestEntity,
                    Map.class
            );

            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
                if (dataList != null && !dataList.isEmpty()) {
                    List<Double> embeddingList = (List<Double>) dataList.get(0).get("embedding");
                    if (embeddingList != null) {
                        float[] vector = new float[embeddingList.size()];
                        for (int i = 0; i < embeddingList.size(); i++) {
                            vector[i] = embeddingList.get(i).floatValue();
                        }
                        return vector;
                    }
                }
            }
            throw new RuntimeException("Invalid embedding response format");
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Embedding API error: " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new RuntimeException("Embedding API request failed: " + e.getMessage(), e);
        }
    }
}