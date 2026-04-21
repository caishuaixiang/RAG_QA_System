package org.example.rag_qa_system.controller;

import org.example.rag_qa_system.entity.Document;
import org.example.rag_qa_system.entity.DocumentChunk;
import org.example.rag_qa_system.entity.QuestionAnswer;
import org.example.rag_qa_system.service.DocumentChunkService;
import org.example.rag_qa_system.service.DocumentService;
import org.example.rag_qa_system.service.QuestionAnswerService;
import org.example.rag_qa_system.service.VectorDatabaseService;
import org.example.rag_qa_system.utils.LLMUtils;
import org.example.rag_qa_system.utils.Result;
import org.example.rag_qa_system.utils.SourceInfo;
import org.example.rag_qa_system.utils.VectorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG问答控制器
 */
@RestController
@RequestMapping("/api/rag")
public class RAGController {

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
    @Qualifier("vectorDatabaseServiceImpl")
    private VectorDatabaseService vectorDatabaseService;

    /**
     * 问答
     * @param question 问题
     * @param userId 用户ID
     * @return 回答结果
     */
    @PostMapping("/ask")
    public Result askQuestion(@RequestParam String question,
                              @RequestParam Long userId) {
        String answer = null;
        String sourceInfo = null;
        String relatedDocumentIds = null;
        String errorMessage = null;

        try {
            // 1. 向量化问题
            float[] questionVector = vectorUtils.getVector(question);

            // 2. 检索相关文档切片
            Map<String, Object> searchResult = searchRelevantChunks(questionVector);
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
     * 检索相关文档切片
     * @param questionVector 问题向量
     * @return 相关文档切片及其来源信息
     */
    private Map<String, Object> searchRelevantChunks(float[] questionVector) {
        // 使用向量数据库检索最相关的3个切片
        List<DocumentChunk> relevantChunks = vectorDatabaseService.searchSimilarChunks(questionVector, 3);

        List<String> chunkContents = new ArrayList<>();
        Map<Long, Document> documentMap = new HashMap<>();
        List<Double> similarities = new ArrayList<>();

        for (DocumentChunk chunk : relevantChunks) {
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

            // 模拟相似度（实际从向量数据库获取）
            similarities.add(0.85 + Math.random() * 0.15);
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
}