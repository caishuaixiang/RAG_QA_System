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
     * @param questionAnswer 问答记录
     * @return 操作结果
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
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 问答历史
     */
    @GetMapping("/history")
    public Result getQuestionAnswerHistory(@RequestParam Long userId,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        List<QuestionAnswer> history = questionAnswerService.getQuestionAnswerHistory(userId, size);

        // 手动实现分页
        int total = history.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);

        List<QuestionAnswer> pageContent = history.subList(fromIndex, toIndex);

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageContent);
        result.put("total", total);
        result.put("size", size);
        result.put("current", page);
        result.put("pages", (int) Math.ceil((double) total / size));

        return Result.success(result);
    }

    /**
     * 删除问答记录
     * @param id 记录ID
     * @return 操作结果
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
}