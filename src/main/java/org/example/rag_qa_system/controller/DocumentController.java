package org.example.rag_qa_system.controller;

import org.example.rag_qa_system.entity.Document;
import org.example.rag_qa_system.service.DocumentService;
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

    /**
     * 上传文档
     * @param file 文档文件
     * @param knowledgeDomain 知识域分类
     * @return 操作结果
     */
    @PostMapping("/upload")
    public Result uploadDocument(@RequestParam("file") MultipartFile file,
                               @RequestParam("knowledgeDomain") String knowledgeDomain) {
        try {
            Document document = documentService.uploadDocument(file, knowledgeDomain);
            return Result.success(document);
        } catch (Exception e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取文档列表
     * @param knowledgeDomain 知识域（可选）
     * @param status 状态（可选）
     * @param page 页码
     * @param size 每页数量
     * @return 文档列表
     */
    @GetMapping("/list")
    public Result getDocumentList(@RequestParam(required = false) String knowledgeDomain,
                                @RequestParam(required = false) Integer status,
                                @RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int size) {
        List<Document> documents = documentService.getDocumentList(knowledgeDomain, status);

        // 手动实现分页
        int total = documents.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);

        List<Document> pageContent = documents.subList(fromIndex, toIndex);

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageContent);
        result.put("total", total);
        result.put("size", size);
        result.put("current", page);
        result.put("pages", (int) Math.ceil((double) total / size));

        return Result.success(result);
    }

    /**
     * 删除文档
     * @param id 文档ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result deleteDocument(@PathVariable Long id) {
        try {
            documentService.deleteDocument(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 处理文档（解析、切片、向量化）
     * @param id 文档ID
     * @return 操作结果
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
}