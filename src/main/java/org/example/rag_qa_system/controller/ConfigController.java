package org.example.rag_qa_system.controller;

import org.example.rag_qa_system.entity.Document;
import org.example.rag_qa_system.service.DocumentService;
import org.example.rag_qa_system.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统配置控制器
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Autowired
    private DocumentService documentService;

    /**
     * 获取系统配置
     * @return 系统配置
     */
    @GetMapping("/system")
    public Result getSystemConfig() {
        Map<String, Object> config = Map.of(
                "vector_db", Map.of(
                        "url", "http://localhost:8000",
                        "collection", "knowledge_base"
                ),
                "llm_api", Map.of(
                        "type", "qwen",
                        "url", "https://dashscope.aliyuncs.com/compatible-mode/v1/text/chat"
                ),
                "embedding_api", Map.of(
                        "url", "https://api.example.com/embedding"
                )
        );
        return Result.success(config);
    }

    /**
     * 更新系统配置
     * @param config 配置信息
     * @return 操作结果
     */
    @PostMapping("/system")
    public Result updateSystemConfig(@RequestBody Map<String, Object> config) {
        // 实际实现中保存配置到数据库或配置文件
        return Result.success("配置更新成功");
    }

    /**
     * 获取知识域列表
     * @return 知识域列表
     */
    @GetMapping("/knowledge-domains")
    public Result getKnowledgeDomains() {
        // 简化版：从文档中提取知识域
        List<Document> documents = documentService.getDocumentList(null, null);
        return Result.success(documents.stream()
                .map(Document::getKnowledgeDomain)
                .distinct()
                .toList());
    }
}