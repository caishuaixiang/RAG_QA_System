package org.example.rag_qa_system.service.impl;

import org.example.rag_qa_system.entity.QuestionAnswer;
import org.example.rag_qa_system.mapper.QuestionAnswerMapper;
import org.example.rag_qa_system.service.QuestionAnswerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 问答服务实现类
 */
@Service
public class QuestionAnswerServiceImpl implements QuestionAnswerService {

    @Autowired
    private QuestionAnswerMapper questionAnswerMapper;

    @Override
    public void saveQuestionAnswer(QuestionAnswer questionAnswer) {
        questionAnswer.setCreateTime(LocalDateTime.now());
        questionAnswer.setUpdateTime(LocalDateTime.now());
        questionAnswer.setStatus(0);
        questionAnswerMapper.insert(questionAnswer);
    }

    @Override
    public QuestionAnswer getQuestionAnswerById(Long id) {
        return questionAnswerMapper.findById(id);
    }

    @Override
    public List<QuestionAnswer> getQuestionAnswerHistory(Long userId, int limit) {
        List<QuestionAnswer> allAnswers = questionAnswerMapper.findByUserId(userId);
        if (allAnswers.size() > limit) {
            return allAnswers.subList(0, limit);
        }
        return allAnswers;
    }

    @Override
    public List<QuestionAnswer> findSimilarQuestions(String question, Long userId) {
        return questionAnswerMapper.findSimilarQuestions(question, userId);
    }

    @Override
    public void deleteQuestionAnswer(Long id) {
        QuestionAnswer questionAnswer = new QuestionAnswer();
        questionAnswer.setId(id);
        questionAnswer.setStatus(1); // 标记为已删除
        questionAnswer.setUpdateTime(LocalDateTime.now());
        questionAnswerMapper.update(questionAnswer);
    }

    @Override
    public void clearHistoryByUserId(Long userId) {
        questionAnswerMapper.deleteByUserId(userId);
    }

    @Override
    public List<QuestionAnswer> searchQuestionAnswer(String keyword, Long userId) {
        List<QuestionAnswer> results;
        if (userId != null) {
            results = questionAnswerMapper.searchByKeywordAndUserId(keyword, userId);
        } else {
            results = questionAnswerMapper.searchByKeyword(keyword);
        }
        // 过滤已删除的记录
        return results.stream()
                .filter(qa -> qa.getStatus() == null || qa.getStatus() == 0)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuestionAnswer> getQuestionAnswersByStatus(Integer status) {
        return questionAnswerMapper.findByStatus(status);
    }
}