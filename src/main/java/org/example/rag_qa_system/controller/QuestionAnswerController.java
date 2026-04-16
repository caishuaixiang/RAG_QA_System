package org.example.rag_qa_system.controller;

import org.example.rag_qa_system.entity.QuestionAnswer;
import org.example.rag_qa_system.service.QuestionAnswerService;
import org.example.rag_qa_system.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 问答控制器
 */
@RestController
@RequestMapping("/api/qa")
public class QuestionAnswerController {

    @Autowired
    private QuestionAnswerService questionAnswerService;

    /**
     * 保存问答记录
     */
    @PostMapping("/save")
    public Result saveQuestionAnswer(@RequestBody QuestionAnswer questionAnswer) {
        try {
            questionAnswerService.saveQuestionAnswer(questionAnswer);
            return Result.success("保存成功");
        } catch (Exception e) {
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    /**
     * 获取问答历史
     */
    @GetMapping("/history")
    public Result getQuestionAnswerHistory(@RequestParam(required = false) Long userId,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        try {
            List<QuestionAnswer> history;
            if (userId != null) {
                history = questionAnswerService.getQuestionAnswerHistory(userId, 100);
            } else {
                history = questionAnswerService.getQuestionAnswersByStatus(0);
            }

            // 手动实现分页
            int total = history.size();
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);

            List<QuestionAnswer> pageContent = history.subList(fromIndex, toIndex);

            Map<String, Object> result = new HashMap<>();
            result.put("list", pageContent);
            result.put("total", total);
            result.put("page", page);
            result.put("size", size);

            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取问答历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取问答详情
     */
    @GetMapping("/{id}")
    public Result getQuestionAnswerDetail(@PathVariable Long id) {
        try {
            QuestionAnswer qa = questionAnswerService.getQuestionAnswerById(id);
            if (qa == null) {
                return Result.error("问答记录不存在");
            }
            return Result.success(qa);
        } catch (Exception e) {
            return Result.error("获取问答详情失败: " + e.getMessage());
        }
    }

    /**
     * 删除问答记录
     */
    @DeleteMapping("/{id}")
    public Result deleteQuestionAnswer(@PathVariable Long id) {
        try {
            questionAnswerService.deleteQuestionAnswer(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 清空问答历史
     */
    @DeleteMapping("/clear/{userId}")
    public Result clearHistory(@PathVariable Long userId) {
        try {
            questionAnswerService.clearHistoryByUserId(userId);
            return Result.success("清空成功");
        } catch (Exception e) {
            return Result.error("清空失败: " + e.getMessage());
        }
    }

    /**
     * 搜索问答记录
     */
    @GetMapping("/search")
    public Result searchQuestionAnswer(@RequestParam String keyword,
                                       @RequestParam(required = false) Long userId,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        try {
            List<QuestionAnswer> results = questionAnswerService.searchQuestionAnswer(keyword, userId);

            // 手动实现分页
            int total = results.size();
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);

            List<QuestionAnswer> pageContent = results.subList(fromIndex, toIndex);

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