package org.example.rag_qa_system.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
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

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 获取文本向量
     * @param text 文本内容
     * @return 向量数组
     */
    public float[] getVector(String text) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("text", text);
            request.put("model", "text-embedding-ada-002");

            Map<String, Object> response = restTemplate.postForObject(
                    embeddingApiUrl,
                    request,
                    Map.class
            );

            if (response != null && response.containsKey("vector")) {
                return (float[]) response.get("vector");
            }
            return new float[0];
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Embedding API error: " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new RuntimeException("Embedding API request failed", e);
        }
    }
}