package org.example.rag_qa_system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.rag_qa_system.entity.QuestionAnswer;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

/**
 * 问答记录Mapper接口
 */
@Mapper
public interface QuestionAnswerMapper {

    /**
     * 根据ID查询问答记录
     */
    @Select("SELECT * FROM question_answer WHERE id = #{id}")
    QuestionAnswer findById(Long id);

    /**
     * 根据用户ID查询问答记录列表
     */
    @Select("SELECT * FROM question_answer WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<QuestionAnswer> findByUserId(Long userId);

    /**
     * 根据问题查询相似的问答记录
     */
    @Select("SELECT * FROM question_answer WHERE question LIKE CONCAT('%', #{question}, '%') " +
            "AND user_id = #{userId} ORDER BY create_time DESC")
    List<QuestionAnswer> findSimilarQuestions(String question, Long userId);

    /**
     * 插入问答记录
     */
    @Insert("INSERT INTO question_answer(user_id, question, answer, source, vector_ids, status, create_time) " +
            "VALUES(#{userId}, #{question}, #{answer}, #{source}, #{vectorIds}, #{status}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuestionAnswer questionAnswer);

    /**
     * 更新问答记录
     */
    @Update("UPDATE question_answer SET question = #{question}, answer = #{answer}, " +
            "source = #{source}, vector_ids = #{vectorIds}, status = #{status} " +
            "WHERE id = #{id}")
    int update(QuestionAnswer questionAnswer);

    /**
     * 删除问答记录
     */
    @Delete("DELETE FROM question_answer WHERE id = #{id}")
    int delete(Long id);

    /**
     * 根据状态查询问答记录
     */
    @Select("SELECT * FROM question_answer WHERE status = #{status} ORDER BY create_time DESC")
    List<QuestionAnswer> findByStatus(Integer status);
}