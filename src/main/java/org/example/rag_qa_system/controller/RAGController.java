package org.example.rag_qa_system.controller;

import org.example.rag_qa_system.dto.SearchResult;
import org.example.rag_qa_system.entity.*;
import org.example.rag_qa_system.service.*;
import org.example.rag_qa_system.utils.LLMUtils;
import org.example.rag_qa_system.utils.Result;
import org.example.rag_qa_system.utils.SourceInfo;
import org.example.rag_qa_system.utils.VectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG问答控制器
 */
@RestController
@RequestMapping("/api/rag")
public class RAGController {

    private static final Logger logger = LoggerFactory.getLogger(RAGController.class);

    @Autowired
    private VectorUtils vectorUtils;

    @Autowired
    private LLMUtils llmUtils;

    @Autowired
    private DocumentChunkService documentChunkService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private QuestionAnswerService questionAnswerService;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    @Qualifier("vectorDatabaseServiceImpl")
    private VectorDatabaseService vectorDatabaseService;

    @Value("${rag.search.topK:5}")
    private int defaultTopK;

    @Value("${rag.history.rounds:3}")
    private int historyRounds;

    /**
     * 问答
     * @param question 问题
     * @param userId 用户ID
     * @param knowledgeBaseId 知识库ID（可选，为null时查询所有知识库）
     * @return 回答结果
     */
    @PostMapping("/ask")
    public Result askQuestion(@RequestParam String question,
                              @RequestParam Long userId,
                              @RequestParam(required = false) Long knowledgeBaseId) {
        String answer = null;
        String sourceInfo = null;
        String relatedDocumentIds = null;
        String errorMessage = null;

        try {
            // 1. 向量化问题
            float[] questionVector = vectorUtils.getVector(question);

            // 2. 检索相关文档切片（支持知识库过滤）
            Map<String, Object> searchResult = searchRelevantChunks(questionVector, knowledgeBaseId);
            List<String> relevantChunks = (List<String>) searchResult.get("chunks");
            sourceInfo = (String) searchResult.get("sourceInfo");
            relatedDocumentIds = (String) searchResult.get("relatedDocumentIds");

            // 3. 生成回答
            String context = String.join("\n", relevantChunks);
            answer = llmUtils.generateAnswer(question, context);

            // 4. 构建返回结果（包含答案和溯源信息）
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("answer", answer);
            responseData.put("sources", SourceInfo.parseSourceInfo(sourceInfo));
            responseData.put("sourceText", SourceInfo.formatSourceInfoAsText(sourceInfo));

            return Result.success(responseData);
        } catch (Exception e) {
            return Result.error("问答失败: " + e.getMessage());
        } finally {
            // 无论成功失败都保存问答记录
            try {
                // 保存问答记录
                QuestionAnswer qa = new QuestionAnswer();
                qa.setUserId(userId);
                qa.setQuestion(question);
                qa.setAnswer(answer != null ? answer : "问答失败: " + errorMessage);
                qa.setVectorIds(relatedDocumentIds);
                qa.setSource(sourceInfo);
                questionAnswerService.saveQuestionAnswer(qa);
            } catch (Exception saveEx){
                // 保存失败不影响主流程
            }
        }
    }

    /**
     * 带对话历史的问答
     * @param question 问题
     * @param userId 用户ID
     * @param conversationId 会话ID（可选，不传则创建新会话）
     * @param knowledgeBaseId 知识库ID（可选，为null时查询所有知识库）
     * @return 回答结果（包含会话ID）
     */
    @PostMapping("/chat")
    public Result chat(@RequestParam String question,
                       @RequestParam Long userId,
                       @RequestParam(required = false) String conversationId,
                       @RequestParam(required = false) Long knowledgeBaseId) {
        String answer = null;
        String sourceInfo = null;
        String relatedDocumentIds = null;

        try {
            // 1. 获取或创建会话
            Conversation conversation;
            if (conversationId == null || conversationId.isEmpty()) {
                conversation = conversationService.createConversation(userId);
                conversationId = conversation.getId();
            } else {
                conversation = conversationService.getConversationById(conversationId);
                if (conversation == null) {
                    return Result.error("会话不存在");
                }
            }

            // 2. 向量化问题
            float[] questionVector = vectorUtils.getVector(question);

            // 3. 检索相关文档切片（支持知识库过滤）
            Map<String, Object> searchResult = searchRelevantChunks(questionVector, knowledgeBaseId);
            List<String> relevantChunks = (List<String>) searchResult.get("chunks");
            sourceInfo = (String) searchResult.get("sourceInfo");
            relatedDocumentIds = (String) searchResult.get("relatedDocumentIds");

            // 4. 获取对话历史
            List<ConversationMessage> historyMessages = conversationService.getRecentMessages(conversationId, historyRounds);
            List<Map<String, String>> history = new ArrayList<>();
            for (ConversationMessage msg : historyMessages) {
                Map<String, String> historyItem = new HashMap<>();
                historyItem.put("role", msg.getRole());
                historyItem.put("content", msg.getContent());
                history.add(historyItem);
            }

            // 5. 生成回答（带历史）
            String context = String.join("\n", relevantChunks);
            answer = llmUtils.generateAnswerWithHistory(question, context, history);

            // 6. 保存对话消息
            conversationService.addMessage(conversationId, "user", question);
            conversationService.addMessage(conversationId, "assistant", answer,sourceInfo);

            // 7. 如果是新会话，用第一个问题作为标题
            if ("新对话".equals(conversation.getTitle()) && question.length() > 0) {
                String title = question.length() > 30 ? question.substring(0, 30) + "..." : question;
                conversationService.updateTitle(conversationId, title);
            }

            // 8. 构建返回结果
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("answer", answer);
            responseData.put("conversationId", conversationId);
            responseData.put("sources", SourceInfo.parseSourceInfo(sourceInfo));
            responseData.put("sourceText", SourceInfo.formatSourceInfoAsText(sourceInfo));

            return Result.success(responseData);
        } catch (Exception e) {
            return Result.error("问答失败: " + e.getMessage());
        } finally {
            // 无论成功失败都保存问答记录到问答历史表
            try {
                QuestionAnswer qa = new QuestionAnswer();
                qa.setUserId(userId);
                qa.setQuestion(question);
                qa.setAnswer(answer != null ? answer : "问答失败");
                qa.setVectorIds(relatedDocumentIds);
                qa.setSource(sourceInfo);
                questionAnswerService.saveQuestionAnswer(qa);
            } catch (Exception saveEx) {
                // 保存失败不影响主流程
            }
        }
    }

    /**
     * 创建新会话
     * @param userId 用户ID
     * @return 会话信息
     */
    @PostMapping("/conversation/create")
    public Result createConversation(@RequestParam Long userId) {
        try {
            Conversation conversation = conversationService.createConversation(userId);
            Map<String, Object> data = new HashMap<>();
            data.put("conversationId", conversation.getId());
            data.put("title", conversation.getTitle());
            data.put("createTime", conversation.getCreateTime());
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("创建会话失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的会话列表
     * @param userId 用户ID
     * @return 会话列表
     */
    @GetMapping("/conversation/list")
    public Result getConversations(@RequestParam Long userId) {
        try {
            List<Conversation> conversations = conversationService.getConversationsByUserId(userId);
            return Result.success(conversations);
        } catch (Exception e) {
            return Result.error("获取会话列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话的消息历史
     * @param conversationId 会话ID
     * @return 消息列表
     */
    @GetMapping("/conversation/messages")
    public Result getMessages(@RequestParam String conversationId) {
        try {
            List<ConversationMessage> messages = conversationService.getMessages(conversationId);
            return Result.success(messages);
        } catch (Exception e) {
            return Result.error("获取消息历史失败: " + e.getMessage());
        }
    }

    /**
     * 删除会话
     * @param conversationId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/conversation/{conversationId}")
    public Result deleteConversation(@PathVariable String conversationId) {
        try {
            conversationService.deleteConversation(conversationId);
            return Result.success("会话已删除");
        } catch (Exception e) {
            return Result.error("删除会话失败: " + e.getMessage());
        }
    }

    /**
     * 清空会话消息历史
     * @param conversationId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/conversation/{conversationId}/messages")
    public Result clearMessages(@PathVariable String conversationId) {
        try {
            conversationService.clearMessages(conversationId);
            return Result.success("消息历史已清空");
        } catch (Exception e) {
            return Result.error("清空消息历史失败: " + e.getMessage());
        }
    }

    /**
     * 检索相关文档切片（带去重）
     * @param questionVector 问题向量
     * @param knowledgeBaseId 知识库ID（可选，为null时查询所有知识库）
     * @return 相关文档切片及其来源信息
     */
    private Map<String, Object> searchRelevantChunks(float[] questionVector, Long knowledgeBaseId) {
        // 使用向量数据库检索最相关的切片（数量由配置决定），获取真实距离值
        List<SearchResult> searchResults = vectorDatabaseService.searchSimilarChunksWithDistance(questionVector, defaultTopK, knowledgeBaseId);
        System.out.println("Found " + searchResults.size() + " relevant chunks from vector DB" + 
                          (knowledgeBaseId != null ? " (knowledgeBaseId: " + knowledgeBaseId + ")" : ""));

        List<String> chunkContents = new ArrayList<>();
        Map<Long, Document> documentMap = new HashMap<>();
        List<Double> similarities = new ArrayList<>();
        List<DocumentChunk> relevantChunks = new ArrayList<>();
        Set<String> addedChunkKeys = new HashSet<>(); // 用于去重

        for (SearchResult searchResult : searchResults) {
            DocumentChunk chunk = searchResult.getChunk();
            
            // 去重：使用 documentId + chunkId 作为 key
            String chunkKey = chunk.getDocumentId() + "_" + chunk.getId();
            if (addedChunkKeys.contains(chunkKey)) {
                continue;
            }
            addedChunkKeys.add(chunkKey);
            
            relevantChunks.add(chunk);
            chunkContents.add(chunk.getChunkContent());

            // 获取文档信息
            if (!documentMap.containsKey(chunk.getDocumentId())) {
                Document document = documentService.getDocumentById(chunk.getDocumentId());
                if (document != null) {
                    documentMap.put(chunk.getDocumentId(), document);
                } else {
                    // 如果文档不存在，创建一个默认文档对象
                    Document defaultDoc = new Document();
                    defaultDoc.setId(chunk.getDocumentId());
                    defaultDoc.setName("文档" + chunk.getDocumentId());
                    defaultDoc.setKnowledgeDomain("未知领域");
                    documentMap.put(chunk.getDocumentId(), defaultDoc);
                }
            }

            // 使用真实距离值计算相似度
            similarities.add(searchResult.getSimilarityPercentage());
        }

        logger.info("Naive RAG: 向量检索 {} 条，去重后 {} 条", searchResults.size(), relevantChunks.size());

        // 创建详细的答案来源信息（包含位置溯源）
        String sourceInfo = SourceInfo.createDetailedSourceInfo(relevantChunks, documentMap, similarities);
        String relatedDocumentIds = SourceInfo.createRelatedChunkIds(relevantChunks);

        Map<String, Object> result = new HashMap<>();
        result.put("chunks", chunkContents);
        result.put("sourceInfo", sourceInfo);
        result.put("relatedDocumentIds", relatedDocumentIds);

        return result;
    }

    /**
     * 获取会话消息数量
     * @param conversationId 会话ID
     * @return 消息数量
     */
    @GetMapping("/conversation/messageCount")
    public Result getMessageCount(@RequestParam String conversationId) {
        try {
            int count = conversationService.getMessageCount(conversationId);
            Map<String, Object> data = new HashMap<>();
            data.put("count", count);
            data.put("rounds", count / 2);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取消息数量失败: " + e.getMessage());
        }
    }

    /**
     * 删除会话最早的N轮对话
     * @param conversationId 会话ID
     * @param rounds 删除轮数（默认10轮）
     * @return 操作结果
     */
    @DeleteMapping("/conversation/{conversationId}/oldest")
    public Result deleteOldestRounds(@PathVariable String conversationId,
                                     @RequestParam(defaultValue = "10") int rounds) {
        try {
            conversationService.deleteOldestRounds(conversationId, rounds);
            int remainingCount = conversationService.getMessageCount(conversationId);
            Map<String, Object> data = new HashMap<>();
            data.put("deletedRounds", rounds);
            data.put("remainingCount", remainingCount);
            data.put("remainingRounds", remainingCount / 2);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("删除对话失败: " + e.getMessage());
        }
    }
}