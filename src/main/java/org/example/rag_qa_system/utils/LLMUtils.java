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
 * 大语言模型工具类
 */
@Component
public class LLMUtils {

    @Value("${llm.api.url}")
    private String llmApiUrl;

    @Value("${llm.api.key}")
    private String llmApiKey;

    @Value("${llm.api.type}")
    private String llmApiType;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 生成回答
     * @param question 问题
     * @param context 上下文信息
     * @return 回答
     */
    public String generateAnswer(String question, String context) {
        try {
            // 准备请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + llmApiKey);

            Map<String, Object> request = new HashMap<>();

            switch (llmApiType.toLowerCase()) {
                case "qwen":
                    // 通义千问API
                    request.put("model", getLLMModel());
                    request.put("messages", new Object[]{
                            new HashMap<String, Object>() {{
                                put("role", "system");
                                put("content", "你是一个智能助手，请根据以下信息回答问题：");
                            }},
                            new HashMap<String, Object>() {{
                                put("role", "user");
                                put("content", "【相关信息】" + context + "\n【问题】" + question + "\n要求：\n1. 只基于上述信息回答，不要编造\n2. 如果信息不足，回答\"暂无相关资料\"\n3. 用简洁中文回答");
                            }}
                    });
                    break;

                case "wenxin":
                    // 文心一言API
                    request.put("model", getLLMModel());
                    request.put("messages", new Object[]{
                            new HashMap<String, Object>() {{
                                put("role", "user");
                                put("content", "【相关信息】" + context + "\n【问题】" + question + "\n要求：\n1. 只基于上述信息回答，不要编造\n2. 如果信息不足，回答\"暂无相关资料\"\n3. 用简洁中文回答");
                            }}
                    });
                    break;

                case "openai":
                    // OpenAI 兼容 API（包括智谱 GLM）
                    request.put("model", getLLMModel());
                    request.put("messages", new Object[]{
                            new HashMap<String, Object>() {{
                                put("role", "system");
                                put("content", "你是一个智能助手，请根据提供的信息回答问题。");
                            }},
                            new HashMap<String, Object>() {{
                                put("role", "user");
                                put("content", "【相关信息】" + context + "\n\n【问题】" + question + "\n\n要求：\n1. 只基于上述信息回答，不要编造\n2. 如果信息不足，回答\"暂无相关资料\"\n3. 用简洁中文回答");
                            }}
                    });
                    break;

                case "chatglm":
                    // 智谱 Anthropic 兼容 API 格式
                    // 使用 x-api-key 而不是 Authorization Bearer
                    headers.set("x-api-key", llmApiKey);
                    headers.remove("Authorization");
                    headers.set("anthropic-version", "2023-06-01");
                    request.put("model", "claude-3-5-sonnet-20241022");
                    request.put("max_tokens", 1024);
                    request.put("messages", new Object[]{
                            new HashMap<String, Object>() {{
                                put("role", "user");
                                put("content", "【相关信息】" + context + "\n\n【问题】" + question + "\n\n要求：\n1. 只基于上述信息回答，不要编造\n2. 如果信息不足，回答\"暂无相关资料\"\n3. 用简洁中文回答");
                            }}
                    });
                    break;

                default:
                    request.put("model", getLLMModel());
                    request.put("messages", new Object[]{
                            new HashMap<String, Object>() {{
                                put("role", "system");
                                put("content", "你是一个智能助手，请根据以下信息回答问题：");
                            }},
                            new HashMap<String, Object>() {{
                                put("role", "user");
                                put("content", "【相关信息】" + context + "\n【问题】" + question + "\n要求：\n1. 只基于上述信息回答，不要编造\n2. 如果信息不足，回答\"暂无相关资料\"\n3. 用简洁中文回答");
                            }}
                    });
            }

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, headers);

            Map<String, Object> response = restTemplate.postForObject(
                    llmApiUrl,
                    requestEntity,
                    Map.class
            );

            System.out.println("LLM API Response: " + response);
            if (response != null) {
                switch (llmApiType.toLowerCase()) {
                    case "qwen":
                        if (response.containsKey("output")) {
                            Map<String, Object> output = (Map<String, Object>) response.get("output");
                            return (String) output.get("text");
                        }
                        break;

                    case "wenxin":
                        if (response.containsKey("result")) {
                            return (String) response.get("result");
                        }
                        break;

                    case "openai":
                        if (response.containsKey("choices")) {
                            List<?> choices = (List<?>) response.get("choices");
                            if (choices != null && !choices.isEmpty()) {
                                Map<String, Object> choice = (Map<String, Object>) choices.get(0);
                                Map<String, Object> message = (Map<String, Object>) choice.get("message");
                                if (message != null) {
                                    return (String) message.get("content");
                                }
                            }
                        }
                        break;

                    case "chatglm":
                        if (response.containsKey("content")) {
                            // Anthropic 格式: {"content": [{"type": "text", "text": "..."}]}
                            List<?> content = (List<?>) response.get("content");
                            if (content != null && !content.isEmpty()) {
                                Map<String, Object> contentItem = (Map<String, Object>) content.get(0);
                                if ("text".equals(contentItem.get("type"))) {
                                    return (String) contentItem.get("text");
                                }
                            }
                        } else if (response.containsKey("choices")) {
                            // OpenAI 兼容格式
                            List<?> choices = (List<?>) response.get("choices");
                            if (choices != null && !choices.isEmpty()) {
                                Map<String, Object> choice = (Map<String, Object>) choices.get(0);
                                Map<String, Object> message = (Map<String, Object>) choice.get("message");
                                if (message != null) {
                                    return (String) message.get("content");
                                }
                            }
                        }
                        break;

                    default:
                        if (response.containsKey("choices")) {
                            List<?> choices = (List<?>) response.get("choices");
                            if (choices != null && !choices.isEmpty()) {
                                Map<String, Object> choice = (Map<String, Object>) choices.get(0);
                                Map<String, Object> message = (Map<String, Object>) choice.get("message");
                                return (String) message.get("content");
                            }
                        }
                }
            }
            return "无法生成回答";
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("LLM API error: " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new RuntimeException("LLM API request failed: " + e.getMessage(), e);
        }
    }

    /**
     * 获取LLM模型名称
     * @return 模型名称
     */
    private String getLLMModel() {
        switch (llmApiType.toLowerCase()) {
            case "qwen":
                return "qwen-turbo";
            case "wenxin":
                return "ERNIE-Bot-turbo";
            case "openai":
                return "glm-4-flash";  // 智谱 GLM-4-Flash 模型
            case "chatglm":
                return "claude-3-5-sonnet-20241022";
            default:
                return "glm-4-flash";
        }
    }
}