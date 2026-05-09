package org.example.rag_qa_system.controller;

import org.example.rag_qa_system.dto.HybridDoc;
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

    @Autowired
    private HybridSearchService hybridSearchService;

    @Autowired
    private RerankService rerankService;

    @Value("${rag.search.topK:5}")
    private int defaultTopK;

    @Value("${rag.history.rounds:3}")
    private int historyRounds;

    @Value("${rag.hybrid.enabled:false}")
    private boolean hybridEnabled;

    @Value("${rag.hybrid.initialK:20}")
    private int hybridInitialK;

    @Value("${rag.hybrid.finalK:10}")
    private int hybridFinalK;

    @Value("${rag.rerank.enabled:false}")
    private boolean rerankEnabled;

    @Value("${siliconflow.rerank.top-n:5}")
    private int rerankTopN;

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
            List<String> relevantChunks;
            Map<String, Object> searchResult;

            if (hybridEnabled) {
                // 混合检索 + 重排序模式
                searchResult = searchRelevantChunksHybrid(question, knowledgeBaseId);
            } else {
                // 原有向量检索模式
                float[] questionVector = vectorUtils.getVector(question);
                searchResult = searchRelevantChunks(questionVector, knowledgeBaseId);
            }

            relevantChunks = (List<String>) searchResult.get("chunks");
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
            logger.error("问答失败", e);
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

            // 2. 检索相关文档切片
            List<String> relevantChunks;
            Map<String, Object> searchResult;

            if (hybridEnabled) {
                // 混合检索 + 重排序模式
                searchResult = searchRelevantChunksHybrid(question, knowledgeBaseId);
            } else {
                // 原有向量检索模式
                float[] questionVector = vectorUtils.getVector(question);
                searchResult = searchRelevantChunks(questionVector, knowledgeBaseId);
            }

            relevantChunks = (List<String>) searchResult.get("chunks");
            sourceInfo = (String) searchResult.get("sourceInfo");
            relatedDocumentIds = (String) searchResult.get("relatedDocumentIds");

            // 3. 获取对话历史
            List<ConversationMessage> historyMessages = conversationService.getRecentMessages(conversationId, historyRounds);
            List<Map<String, String>> history = new ArrayList<>();
            for (ConversationMessage msg : historyMessages) {
                Map<String, String> historyItem = new HashMap<>();
                historyItem.put("role", msg.getRole());
                historyItem.put("content", msg.getContent());
                history.add(historyItem);
            }

            // 4. 生成回答（带历史）
            String context = String.join("\n", relevantChunks);
            answer = llmUtils.generateAnswerWithHistory(question, context, history);

            // 5. 保存对话消息
            conversationService.addMessage(conversationId, "user", question);
            conversationService.addMessage(conversationId, "assistant", answer,sourceInfo);

            // 6. 如果是新会话，用第一个问题作为标题
            if ("新对话".equals(conversation.getTitle()) && question.length() > 0) {
                String title = question.length() > 30 ? question.substring(0, 30) + "..." : question;
                conversationService.updateTitle(conversationId, title);
            }

            // 7. 构建返回结果
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("answer", answer);
            responseData.put("conversationId", conversationId);
            responseData.put("sources", SourceInfo.parseSourceInfo(sourceInfo));
            responseData.put("sourceText", SourceInfo.formatSourceInfoAsText(sourceInfo));

            return Result.success(responseData);
        } catch (Exception e) {
            logger.error("问答失败", e);
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
     * 检索相关文档切片
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

        for (SearchResult searchResult : searchResults) {
            DocumentChunk chunk = searchResult.getChunk();
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
     * 检索相关文档切片（混合检索 + 重排序模式）
     * 流程：混合检索(BM25+向量) → RRF融合 → 重排序 → 取topN
     * @param question 问题文本
     * @param knowledgeBaseId 知识库ID（暂未使用，保留接口兼容）
     * @return 相关文档切片及其来源信息
     */
    private Map<String, Object> searchRelevantChunksHybrid(String question, Long knowledgeBaseId) {
        logger.info("混合检索模式启动，question='{}', knowledgeBaseId={}", question, knowledgeBaseId);

        // 1. 执行混合检索（BM25 + 向量检索，RRF融合，支持知识库隔离）
        List<HybridDoc> hybridDocs = hybridSearchService.hybridSearch(question, hybridInitialK, hybridFinalK, knowledgeBaseId);
        logger.info("混合检索完成，返回 {} 条候选文档", hybridDocs.size());

        // 2. 重排序（如果启用）
        List<String> finalContents;
        if (rerankEnabled && !hybridDocs.isEmpty()) {
            List<String> candidateContents = new ArrayList<>();
            for (HybridDoc doc : hybridDocs) {
                candidateContents.add(doc.getContent());
            }

            logger.info("开始重排序，候选文档 {} 条", candidateContents.size());
            finalContents = rerankService.rerankDocuments(question, candidateContents);
            logger.info("重排序完成，返回 {} 条结果", finalContents.size());
        } else {
            // 不启用重排序时，直接使用混合检索结果
            finalContents = new ArrayList<>();
            for (HybridDoc doc : hybridDocs) {
                finalContents.add(doc.getContent());
            }
        }

        // 3. 构建溯源信息（带去重）
        // 只使用最终用于生成回答的文档（finalContents对应的）
        List<DocumentChunk> relevantChunks = new ArrayList<>();
        Map<Long, Document> documentMap = new HashMap<>();
        List<Double> similarities = new ArrayList<>();
        Set<String> addedChunkKeys = new HashSet<>(); // 用于去重：documentId_chunkId

        // 构建content到HybridDoc的映射，用于获取相似度和命中标记
        Map<String, HybridDoc> contentDocMap = new HashMap<>();
        for (HybridDoc hd : hybridDocs) {
            contentDocMap.put(hd.getContent(), hd);
        }

        // 只对finalContents中的文档构建溯源信息（去重处理）
        for (String content : finalContents) {
            HybridDoc hd = contentDocMap.get(content);
            if (hd == null) continue;

            // 生成去重key
            String chunkKey = hd.getSourceDocId() + "_" + hd.getDocId();
            if (addedChunkKeys.contains(chunkKey)) {
                logger.debug("跳过重复来源: {}", chunkKey);
                continue;
            }
            addedChunkKeys.add(chunkKey);

            // 添加相似度
            if (hd.isVectorHit()) {
                similarities.add(hd.getVectorSimilarity());
            } else {
                // BM25独有文档，标记为关键词命中（存储为负数，前端识别显示"关键词命中"）
                similarities.add(-1.0);
            }

            // 获取chunk信息
            try {
                String docIdStr = hd.getDocId();
                if (docIdStr != null && !docIdStr.isEmpty()) {
                    Long chunkId = Long.parseLong(docIdStr);
                    DocumentChunk chunk = documentChunkService.getChunksByDocumentId(hd.getSourceDocId())
                            .stream()
                            .filter(c -> c.getId().equals(chunkId))
                            .findFirst()
                            .orElseGet(() -> {
                                DocumentChunk c = new DocumentChunk();
                                c.setId(chunkId);
                                c.setChunkContent(content);
                                c.setDocumentId(hd.getSourceDocId());
                                return c;
                            });
                    relevantChunks.add(chunk);

                    // 获取文档信息
                    if (hd.getSourceDocId() != null && !documentMap.containsKey(hd.getSourceDocId())) {
                        Document document = documentService.getDocumentById(hd.getSourceDocId());
                        if (document != null) {
                            documentMap.put(hd.getSourceDocId(), document);
                        } else {
                            Document defaultDoc = new Document();
                            defaultDoc.setId(hd.getSourceDocId());
                            defaultDoc.setName("文档" + hd.getSourceDocId());
                            defaultDoc.setKnowledgeDomain("未知领域");
                            documentMap.put(hd.getSourceDocId(), defaultDoc);
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("构建溯源信息时异常", e);
            }
        }

        logger.info("构建溯源信息完成，去重后 {} 个来源 (原始 {} 个)", relevantChunks.size(), finalContents.size());

        // 创建详细的答案来源信息
        String sourceInfo = SourceInfo.createDetailedSourceInfo(relevantChunks, documentMap, similarities);
        String relatedDocumentIds = SourceInfo.createRelatedChunkIds(relevantChunks);

        Map<String, Object> result = new HashMap<>();
        result.put("chunks", finalContents);
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