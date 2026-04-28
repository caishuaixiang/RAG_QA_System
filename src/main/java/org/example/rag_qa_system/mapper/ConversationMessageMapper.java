package org.example.rag_qa_system.mapper;

import org.apache.ibatis.annotations.*;
import org.example.rag_qa_system.entity.ConversationMessage;

import java.util.List;

/**
 * 对话消息Mapper接口
 */
@Mapper
public interface ConversationMessageMapper {

    /**
     * 插入消息
     */
    @Insert("INSERT INTO conversation_message (conversation_id, role, content, sources, create_time) " +
            "VALUES (#{conversationId}, #{role}, #{content}, #{sources}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ConversationMessage message);

    /**
     * 根据会话ID查询消息列表
     */
    @Select("SELECT id, conversation_id, role, content, sources, create_time FROM conversation_message WHERE conversation_id = #{conversationId} ORDER BY create_time ASC")
    List<ConversationMessage> findByConversationId(@Param("conversationId") String conversationId);

    /**
     * 根据会话ID查询最近N条消息
     */
    @Select("SELECT id, conversation_id, role, content, sources, create_time FROM conversation_message WHERE conversation_id = #{conversationId} " +
            "ORDER BY create_time DESC LIMIT #{limit}")
    List<ConversationMessage> findRecentByConversationId(@Param("conversationId") String conversationId,
                                                         @Param("limit") int limit);

    /**
     * 根据会话ID删除所有消息
     */
    @Delete("DELETE FROM conversation_message WHERE conversation_id = #{conversationId}")
    void deleteByConversationId(@Param("conversationId") String conversationId);

    /**
     * 统计会话消息数量
     */
    @Select("SELECT COUNT(*) FROM conversation_message WHERE conversation_id = #{conversationId}")
    int countByConversationId(@Param("conversationId") String conversationId);

    /**
     * 删除会话最早的N条消息
     */
    @Delete("DELETE FROM conversation_message WHERE conversation_id = #{conversationId} " +
            "ORDER BY create_time ASC LIMIT #{limit}")
    void deleteOldestMessages(@Param("conversationId") String conversationId, @Param("limit") int limit);
}