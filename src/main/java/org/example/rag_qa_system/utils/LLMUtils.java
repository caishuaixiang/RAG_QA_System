package org.example.rag_qa_system.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
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

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 生成回答
     * @param question 问题
     * @param context 上下文信息
     * @return 回答
     */
    public String generateAnswer(String question, String context) {
        try {
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

                case "chatglm":
                    // ChatGLM API
                    request.put("model", getLLMModel());
                    request.put("messages", new Object[]{
                            new HashMap<String, Object>() {{
                                put("role", "user");
                                put("content", "【相关信息】" + context + "\n【问题】" + question + "\n要求：\n1. 只基于上述信息回答，不要编造\n2. 如果信息不足，回答\"暂无相关资料\"\n3. 用简洁中文回答");
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

            Map<String, Object> response = restTemplate.postForObject(
                    llmApiUrl,
                    request,
                    Map.class
            );

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

                    case "chatglm":
                        if (response.containsKey("choices")) {
                            Object[] choices = (Object[]) response.get("choices");
                            if (choices.length > 0) {
                                Map<String, Object> choice = (Map<String, Object>) choices[0];
                                return (String) choice.get("content");
                            }
                        }
                        break;

                    default:
                        if (response.containsKey("choices")) {
                            Object[] choices = (Object[]) response.get("choices");
                            if (choices.length > 0) {
                                Map<String, Object> choice = (Map<String, Object>) choices[0];
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
            throw new RuntimeException("LLM API request failed", e);
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
            case "chatglm":
                return "chatglm3";
            default:
                return "qwen-turbo";
        }
    }
}