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

import java.util.ArrayList;
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
                                put("content", "【相关信息】" + context + "\n【问题】" + question + "\n要求：\n1. 仅依据提供的相关信息作答，严禁编造、扩充、联想内容\n2. 信息不足时，统一回复：暂无相关资料\n3. 回答准确完整、语句通顺，无需刻意精简");
                            }}
                    });
                    break;

                case "wenxin":
                    // 文心一言API
                    request.put("model", getLLMModel());
                    request.put("messages", new Object[]{
                            new HashMap<String, Object>() {{
                                put("role", "user");
                                put("content", "【相关信息】" + context + "\n【问题】" + question + "\n要求：\n1. 仅依据提供的相关信息作答，严禁编造、扩充、联想内容\n2. 信息不足时，统一回复：暂无相关资料\n3. 回答准确完整、语句通顺，无需刻意精简");
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
                                put("content", "【相关信息】" + context + "\n【问题】" + question + "\n要求：\n1. 仅依据提供的相关信息作答，严禁编造、扩充、联想内容\n2. 信息不足时，统一回复：暂无相关资料\n3. 回答准确完整、语句通顺，无需刻意精简");
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
                                put("content", "【相关信息】" + context + "\n【问题】" + question + "\n要求：\n1. 仅依据提供的相关信息作答，严禁编造、扩充、联想内容\n2. 信息不足时，统一回复：暂无相关资料\n3. 回答准确完整、语句通顺，无需刻意精简");
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
                                put("content", "【相关信息】" + context + "\n【问题】" + question + "\n要求：\n1. 仅依据提供的相关信息作答，严禁编造、扩充、联想内容\n2. 信息不足时，统一回复：暂无相关资料\n3. 回答准确完整、语句通顺，无需刻意精简");
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
     * 生成回答（带对话历史）
     * @param question 问题
     * @param context 上下文信息
     * @param history 对话历史（List<Map<String, String>>，每个Map包含role和content）
     * @return 回答
     */
    public String generateAnswerWithHistory(String question, String context, List<Map<String, String>> history) {
        try {
            // 准备请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + llmApiKey);

            Map<String, Object> request = new HashMap<>();
            List<Map<String, Object>> messages = new ArrayList<>();

            // 添加系统提示
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个专业的问答助手。请注意：\n" +
                    "1. 严格基于提供的【相关信息】回答当前【问题】\n" +
                    "2. 仔细理解用户当前问题的真实意图，不要假设用户在问之前问过的问题\n" +
                    "3. 如果用户问的是新问题（如怎么销假），请回答这个新问题，而不是重复回答之前的问题（如如何请假）\n" +
                    "4. 【相关信息】中可能包含多个来源，请只使用与当前问题相关的内容\n" +
                    "5. 如果相关信息不足以回答当前问题，请回复：暂无相关资料");
            messages.add(systemMessage);

            // 添加对话历史
            if (history != null && !history.isEmpty()) {
                for (Map<String, String> msg : history) {
                    Map<String, Object> historyMessage = new HashMap<>();
                    historyMessage.put("role", msg.get("role"));
                    historyMessage.put("content", msg.get("content"));
                    messages.add(historyMessage);
                }
            }

            // 添加当前问题
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", "【对话历史摘要】以下是之前的对话，请理解上下文但重点关注当前问题：\n" +
                    buildHistorySummary(history) + "\n\n" +
                    "【相关信息】以下是从知识库中检索到的与当前问题相关的内容：\n" + context + "\n\n" +
                    "【当前问题】" + question + "\n\n" +
                    "【回答要求】\n" +
                    "1. 只回答当前【问题】，不要回答对话历史中的其他问题\n" +
                    "2. 严格基于【相关信息】作答，禁止编造内容\n" +
                    "3. 如果【相关信息】中没有与当前问题相关的内容，回复：暂无相关资料\n" +
                    "4. 回答要准确、完整、条理清晰");
            messages.add(userMessage);

            request.put("model", getLLMModel());
            request.put("messages", messages);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, headers);

            Map<String, Object> response = restTemplate.postForObject(
                    llmApiUrl,
                    requestEntity,
                    Map.class
            );

            System.out.println("LLM API Response: " + response);
            return extractResponseContent(response);

        } catch (HttpClientErrorException e) {
            throw new RuntimeException("LLM API error: " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new RuntimeException("LLM API request failed: " + e.getMessage(), e);
        }
    }

    /**
     * 从LLM响应中提取内容
     */
    private String extractResponseContent(Map<String, Object> response) {
        if (response == null) {
            return "无法生成回答";
        }

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
            default:
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
        }
        return "无法生成回答";
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

    /**
     * 构建对话历史摘要（限制长度，避免token超限）
     * @param history 对话历史
     * @return 历史摘要字符串
     */
    private String buildHistorySummary(List<Map<String, String>> history) {
        if (history == null || history.isEmpty()) {
            return "（无对话历史）";
        }

        StringBuilder sb = new StringBuilder();
        // 只取最近的几轮对话
        int maxHistoryItems = 4; // 最多2轮对话（4条消息）
        int startIndex = Math.max(0, history.size() - maxHistoryItems);

        for (int i = startIndex; i < history.size(); i++) {
            Map<String, String> msg = history.get(i);
            String role = "user".equals(msg.get("role")) ? "用户" : "助手";
            String content = msg.get("content");
            // 截断过长的内容
            if (content != null && content.length() > 200) {
                content = content.substring(0, 200) + "...";
            }
            sb.append(role).append("：").append(content).append("\n");
        }

        return sb.toString().trim();
    }
}