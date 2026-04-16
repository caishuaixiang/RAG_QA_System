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
    @Select("SELECT * FROM question_answer WHERE user_id = #{userId} AND (status IS NULL OR status = 0) ORDER BY create_time DESC")
    List<QuestionAnswer> findByUserId(Long userId);

    /**
     * 根据问题查询相似的问答记录
     */
    @Select("SELECT * FROM question_answer WHERE question LIKE CONCAT('%', #{question}, '%') " +
            "AND user_id = #{userId} AND (status IS NULL OR status = 0) ORDER BY create_time DESC")
    List<QuestionAnswer> findSimilarQuestions(String question, Long userId);

    /**
     * 插入问答记录
     */
    @Insert("INSERT INTO question_answer(user_id, question, answer, source, vector_ids, status, create_time, update_time) " +
            "VALUES(#{userId}, #{question}, #{answer}, #{source}, #{vectorIds}, #{status}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuestionAnswer questionAnswer);

    /**
     * 更新问答记录
     */
    @Update("UPDATE question_answer SET question = #{question}, answer = #{answer}, " +
            "source = #{source}, vector_ids = #{vectorIds}, status = #{status}, update_time = #{updateTime} " +
            "WHERE id = #{id}")
    int update(QuestionAnswer questionAnswer);

    /**
     * 删除问答记录（物理删除）
     */
    @Delete("DELETE FROM question_answer WHERE id = #{id}")
    int delete(Long id);

    /**
     * 根据用户ID删除所有问答记录
     */
    @Delete("DELETE FROM question_answer WHERE user_id = #{userId}")
    int deleteByUserId(Long userId);

    /**
     * 根据状态查询问答记录
     */
    @Select("SELECT * FROM question_answer WHERE status = #{status} ORDER BY create_time DESC")
    List<QuestionAnswer> findByStatus(Integer status);

    /**
     * 根据关键词搜索问答记录
     */
    @Select("SELECT * FROM question_answer WHERE (question LIKE CONCAT('%', #{keyword}, '%') " +
            "OR answer LIKE CONCAT('%', #{keyword}, '%')) AND (status IS NULL OR status = 0) " +
            "ORDER BY create_time DESC")
    List<QuestionAnswer> searchByKeyword(String keyword);

    /**
     * 根据关键词和用户ID搜索问答记录
     */
    @Select("SELECT * FROM question_answer WHERE user_id = #{userId} AND (question LIKE CONCAT('%', #{keyword}, '%') " +
            "OR answer LIKE CONCAT('%', #{keyword}, '%')) AND (status IS NULL OR status = 0) " +
            "ORDER BY create_time DESC")
    List<QuestionAnswer> searchByKeywordAndUserId(String keyword, Long userId);

    /**
     * 统计问答记录数量
     */
    @Select("SELECT COUNT(*) FROM question_answer WHERE (status IS NULL OR status = 0)")
    int count();

    /**
     * 根据用户ID统计问答记录数量
     */
    @Select("SELECT COUNT(*) FROM question_answer WHERE user_id = #{userId} AND (status IS NULL OR status = 0)")
    int countByUserId(Long userId);
}