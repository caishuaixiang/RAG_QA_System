package org.example.rag_qa_system.mapper;

import org.apache.ibatis.annotations.*;
import org.example.rag_qa_system.entity.Conversation;

import java.util.List;

/**
 * 对话会话Mapper接口
 */
@Mapper
public interface ConversationMapper {

    /**
     * 插入会话
     */
    @Insert("INSERT INTO conversation (id, user_id, title, create_time, update_time, status) " +
            "VALUES (#{id}, #{userId}, #{title}, #{createTime}, #{updateTime}, #{status})")
    void insert(Conversation conversation);

    /**
     * 根据ID查询会话
     */
    @Select("SELECT * FROM conversation WHERE id = #{id}")
    Conversation findById(@Param("id") String id);

    /**
     * 根据用户ID查询会话列表
     */
    @Select("SELECT * FROM conversation WHERE user_id = #{userId} ORDER BY update_time DESC")
    List<Conversation> findByUserId(@Param("userId") Long userId);

    /**
     * 更新会话
     */
    @Update("UPDATE conversation SET title = #{title}, update_time = #{updateTime}, status = #{status} WHERE id = #{id}")
    void update(Conversation conversation);

    /**
     * 删除会话
     */
    @Delete("DELETE FROM conversation WHERE id = #{id}")
    void delete(@Param("id") String id);

    /**
     * 根据用户ID删除所有会话
     */
    @Delete("DELETE FROM conversation WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);
}