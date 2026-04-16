package org.example.rag_qa_system.controller;

import org.example.rag_qa_system.entity.KnowledgeBase;
import org.example.rag_qa_system.service.KnowledgeBaseService;
import org.example.rag_qa_system.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库管理控制器
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    /**
     * 获取知识库列表
     */
    @GetMapping("/list")
    public Result getKnowledgeList(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer size) {
        try {
            List<KnowledgeBase> list;
            if (userId != null) {
                list = knowledgeBaseService.getKnowledgeBasesByUserId(userId);
            } else if (name != null || status != null) {
                list = knowledgeBaseService.searchKnowledgeBases(userId, name, status);
            } else {
                list = knowledgeBaseService.getAllKnowledgeBases();
            }

            // 简单分页
            int total = list.size();
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);
            List<KnowledgeBase> pageList = list.subList(fromIndex, toIndex);

            Map<String, Object> data = new HashMap<>();
            data.put("list", pageList);
            data.put("total", total);
            data.put("page", page);
            data.put("size", size);

            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取知识库列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取知识库详情
     */
    @GetMapping("/{id}")
    public Result getKnowledgeDetail(@PathVariable Long id) {
        try {
            KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBaseById(id);
            if (knowledgeBase == null) {
                return Result.error("知识库不存在");
            }
            return Result.success(knowledgeBase);
        } catch (Exception e) {
            return Result.error("获取知识库详情失败: " + e.getMessage());
        }
    }

    /**
     * 创建知识库
     */
    @PostMapping
    public Result createKnowledgeBase(@RequestBody KnowledgeBase knowledgeBase) {
        try {
            KnowledgeBase created = knowledgeBaseService.createKnowledgeBase(knowledgeBase);
            return Result.success(created);
        } catch (Exception e) {
            return Result.error("创建知识库失败: " + e.getMessage());
        }
    }

    /**
     * 更新知识库
     */
    @PutMapping("/{id}")
    public Result updateKnowledgeBase(@PathVariable Long id, @RequestBody KnowledgeBase knowledgeBase) {
        try {
            knowledgeBase.setId(id);
            KnowledgeBase updated = knowledgeBaseService.updateKnowledgeBase(knowledgeBase);
            return Result.success(updated);
        } catch (Exception e) {
            return Result.error("更新知识库失败: " + e.getMessage());
        }
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{id}")
    public Result deleteKnowledgeBase(@PathVariable Long id) {
        try {
            knowledgeBaseService.deleteKnowledgeBase(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("删除知识库失败: " + e.getMessage());
        }
    }

    /**
     * 获取知识库统计信息
     */
    @GetMapping("/{id}/stats")
    public Result getKnowledgeStats(@PathVariable Long id) {
        try {
            KnowledgeBaseService.KnowledgeBaseStats stats = knowledgeBaseService.getKnowledgeBaseStats(id);
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取知识库下的文档列表
     */
    @GetMapping("/{id}/documents")
    public Result getKnowledgeDocuments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            // 这里可以调用DocumentService获取文档列表
            return Result.success("功能待实现");
        } catch (Exception e) {
            return Result.error("获取文档列表失败: " + e.getMessage());
        }
    }
}