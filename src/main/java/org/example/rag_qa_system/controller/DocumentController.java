package org.example.rag_qa_system.controller;

import org.example.rag_qa_system.entity.Document;
import org.example.rag_qa_system.entity.DocumentChunk;
import org.example.rag_qa_system.service.DocumentService;
import org.example.rag_qa_system.service.DocumentChunkService;
import org.example.rag_qa_system.service.KnowledgeBaseService;
import org.example.rag_qa_system.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档管理控制器
 */
@RestController
@RequestMapping("/api/document")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentChunkService documentChunkService;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    /**
     * 上传文档
     */
    @PostMapping("/upload")
    public Result uploadDocument(@RequestParam("file") MultipartFile file,
                                 @RequestParam("knowledgeDomain") String knowledgeDomain,
                                 @RequestParam(value = "userId", required = false) Long userId) {
        try {
            Document document = documentService.uploadDocument(file, knowledgeDomain);
            if (userId != null) {
                document.setUserId(userId);
                documentService.updateDocument(document);
            }
            // 更新知识库的文档数量
            if (knowledgeDomain != null && !knowledgeDomain.isEmpty()) {
                try {
                    Long knowledgeBaseId = Long.parseLong(knowledgeDomain);
                    knowledgeBaseService.updateDocumentCount(knowledgeBaseId);
                } catch (NumberFormatException e) {
                    // knowledgeDomain 不是数字，忽略
                }
            }
            return Result.success(document);
        } catch (Exception e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取文档列表
     */
    @GetMapping("/list")
    public Result getDocumentList(@RequestParam(required = false) String knowledgeDomain,
                                  @RequestParam(required = false) Long knowledgeId,
                                  @RequestParam(required = false) Integer status,
                                  @RequestParam(required = false) Long userId,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        try {
            List<Document> documents;

            // 优先使用 knowledgeId，其次使用 knowledgeDomain
            String domain = knowledgeId != null ? String.valueOf(knowledgeId) : knowledgeDomain;

            if (domain != null && !domain.isEmpty()) {
                documents = documentService.getDocumentList(domain, status);
            } else if (status != null) {
                documents = documentService.getDocumentList(null, status);
            } else if (keyword != null && !keyword.isEmpty()) {
                documents = documentService.searchDocuments(keyword);
            } else {
                documents = documentService.getDocumentList(null, null);
            }

            // 手动实现分页
            int total = documents.size();
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);

            List<Document> pageContent = documents.subList(fromIndex, toIndex);

            Map<String, Object> result = new HashMap<>();
            result.put("list", pageContent);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);

            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取文档列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/{id}")
    public Result getDocumentDetail(@PathVariable Long id) {
        try {
            Document document = documentService.getDocumentById(id);
            if (document == null) {
                return Result.error("文档不存在");
            }
            return Result.success(document);
        } catch (Exception e) {
            return Result.error("获取文档详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取文档内容预览
     */
    @GetMapping("/{id}/content")
    public Result getDocumentContent(@PathVariable Long id) {
        try {
            Document document = documentService.getDocumentById(id);
            if (document == null) {
                return Result.error("文档不存在");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("id", document.getId());
            result.put("name", document.getName());
            result.put("type", document.getType());
            result.put("content", document.getContent());

            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取文档内容失败: " + e.getMessage());
        }
    }
    /**
     * 获取文档切片列表
     */
    @GetMapping("/{id}/chunks")
    public Result getDocumentChunks(@PathVariable Long id,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        try {
            List<DocumentChunk> chunks = documentChunkService.getChunksByDocumentId(id);

            // 手动实现分页
            int total = chunks.size();
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);

            List<DocumentChunk> pageContent = chunks.subList(fromIndex, toIndex);

            Map<String, Object> result = new HashMap<>();
            result.put("list", pageContent);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);

            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取切片列表失败: " + e.getMessage());
        }
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{id}")
    public Result deleteDocument(@PathVariable Long id) {
        try {
            // 删除前获取文档信息，用于更新知识库文档数量
            Document document = documentService.getDocumentById(id);
            String knowledgeDomain = document != null ? document.getKnowledgeDomain() : null;

            documentService.deleteDocument(id);

            // 更新知识库的文档数量
            if (knowledgeDomain != null && !knowledgeDomain.isEmpty()) {
                try {
                    Long knowledgeBaseId = Long.parseLong(knowledgeDomain);
                    knowledgeBaseService.updateDocumentCount(knowledgeBaseId);
                } catch (NumberFormatException e) {
                    // knowledgeDomain 不是数字，忽略
                }
            }
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除文档
     */
    @PostMapping("/batch-delete")
    public Result batchDeleteDocuments(@RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> ids = request.get("ids");
            if (ids == null || ids.isEmpty()) {
                return Result.error("请选择要删除的文档");
            }

            // 收集需要更新的知识库ID
            java.util.Set<Long> knowledgeBaseIds = new java.util.HashSet<>();

            for (Long id : ids) {
                Document document = documentService.getDocumentById(id);
                if (document != null && document.getKnowledgeDomain() != null) {
                    try {
                        Long knowledgeBaseId = Long.parseLong(document.getKnowledgeDomain());
                        knowledgeBaseIds.add(knowledgeBaseId);
                    } catch (NumberFormatException e) {
                        // 忽略
                    }
                }
                documentService.deleteDocument(id);
            }

            // 更新所有受影响的知识库文档数量
            for (Long knowledgeBaseId : knowledgeBaseIds) {
                knowledgeBaseService.updateDocumentCount(knowledgeBaseId);
            }

            return Result.success("批量删除成功");
        } catch (Exception e) {
            return Result.error("批量删除失败: " + e.getMessage());
        }
    }

    /**
     * 处理文档（解析、切片、向量化）
     */
    @PostMapping("/process/{id}")
    public Result processDocument(@PathVariable Long id) {
        try {
            documentService.processDocument(id);
            return Result.success("处理成功");
        } catch (Exception e) {
            return Result.error("处理失败: " + e.getMessage());
        }
    }

    /**
     * 搜索文档
     */
    @GetMapping("/search")
    public Result searchDocuments(@RequestParam String keyword,
                                  @RequestParam(required = false) Long userId,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        try {
            List<Document> documents = documentService.searchDocuments(keyword);

            // 手动实现分页
            int total = documents.size();
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);

            List<Document> pageContent = documents.subList(fromIndex, toIndex);

            Map<String, Object> result = new HashMap<>();
            result.put("list", pageContent);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);

            return Result.success(result);
        } catch (Exception e) {
            return Result.error("搜索失败: " + e.getMessage());
        }
    }
}