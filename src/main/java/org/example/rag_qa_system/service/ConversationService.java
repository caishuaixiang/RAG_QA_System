package org.example.rag_qa_system.service;

import org.example.rag_qa_system.entity.Conversation;
import org.example.rag_qa_system.entity.ConversationMessage;

import java.util.List;

/**
 * 对话会话服务接口
 */
public interface ConversationService {

    /**
     * 创建新会话
     * @param userId 用户ID
     * @return 会话对象
     */
    Conversation createConversation(Long userId);

    /**
     * 根据ID获取会话
     * @param conversationId 会话ID
     * @return 会话对象
     */
    Conversation getConversationById(String conversationId);

    /**
     * 获取用户的所有会话
     * @param userId 用户ID
     * @return 会话列表
     */
    List<Conversation> getConversationsByUserId(Long userId);

    /**
     * 更新会话
     * @param conversation 会话对象
     */
    void updateConversation(Conversation conversation);

    /**
     * 删除会话（包括所有消息）
     * @param conversationId 会话ID
     */
    void deleteConversation(String conversationId);

    /**
     * 添加消息到会话
     * @param conversationId 会话ID
     * @param role 角色（user/assistant）
     * @param content 消息内容
     * @return 消息对象
     */
    ConversationMessage addMessage(String conversationId, String role, String content);

    /**
     * 添加消息到会话（带来源信息）
     * @param conversationId 会话ID
     * @param role 角色（user/assistant）
     * @param content 消息内容
     * @param sources 答案来源信息（JSON格式）
     * @return 消息对象
     */
    ConversationMessage addMessage(String conversationId, String role, String content, String sources);

    /**
     * 获取会话的消息历史
     * @param conversationId 会话ID
     * @return 消息列表
     */
    List<ConversationMessage> getMessages(String conversationId);

    /**
     * 获取会话最近N轮对话历史（用于上下文）
     * @param conversationId 会话ID
     * @param rounds 对话轮数（一轮=一问一答）
     * @return 消息列表
     */
    List<ConversationMessage> getRecentMessages(String conversationId, int rounds);

    /**
     * 清空会话消息历史
     * @param conversationId 会话ID
     */
    void clearMessages(String conversationId);

    /**
     * 更新会话标题
     * @param conversationId 会话ID
     * @param title 标题
     */
    void updateTitle(String conversationId, String title);

    /**
     * 获取会话消息数量
     * @param conversationId 会话ID
     * @return 消息数量
     */
    int getMessageCount(String conversationId);

    /**
     * 删除会话最早的N轮对话
     * @param conversationId 会话ID
     * @param rounds 对话轮数
     */
    void deleteOldestRounds(String conversationId, int rounds);
}