package org.example.rag_qa_system.service;

import org.example.rag_qa_system.entity.QuestionAnswer;

import java.util.List;

/**
 * 问答服务接口
 */
public interface QuestionAnswerService {

    /**
     * 保存问答记录
     * @param questionAnswer 问答记录
     */
    void saveQuestionAnswer(QuestionAnswer questionAnswer);

    /**
     * 根据ID获取问答记录
     * @param id 记录ID
     * @return 问答记录
     */
    QuestionAnswer getQuestionAnswerById(Long id);

    /**
     * 获取问答历史
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 问答历史列表
     */
    List<QuestionAnswer> getQuestionAnswerHistory(Long userId, int limit);

    /**
     * 查找相似问题
     * @param question 问题
     * @param userId 用户ID
     * @return 相似问题列表
     */
    List<QuestionAnswer> findSimilarQuestions(String question, Long userId);

    /**
     * 删除问答记录
     * @param id 记录ID
     */
    void deleteQuestionAnswer(Long id);

    /**
     * 清空用户问答历史
     * @param userId 用户ID
     */
    void clearHistoryByUserId(Long userId);

    /**
     * 搜索问答记录
     * @param keyword 关键词
     * @param userId 用户ID（可选）
     * @return 问答记录列表
     */
    List<QuestionAnswer> searchQuestionAnswer(String keyword, Long userId);

    /**
     * 根据状态获取问答记录
     * @param status 状态
     * @return 问答记录列表
     */
    List<QuestionAnswer> getQuestionAnswersByStatus(Integer status);
}