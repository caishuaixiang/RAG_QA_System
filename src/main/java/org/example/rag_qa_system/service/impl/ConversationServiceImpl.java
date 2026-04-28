package org.example.rag_qa_system.service.impl;

import org.example.rag_qa_system.entity.Conversation;
import org.example.rag_qa_system.entity.ConversationMessage;
import org.example.rag_qa_system.mapper.ConversationMapper;
import org.example.rag_qa_system.mapper.ConversationMessageMapper;
import org.example.rag_qa_system.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 对话会话服务实现类
 */
@Service
public class ConversationServiceImpl implements ConversationService {

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private ConversationMessageMapper messageMapper;

    @Override
    public Conversation createConversation(Long userId) {
        Conversation conversation = new Conversation();
        conversation.setId(UUID.randomUUID().toString());
        conversation.setUserId(userId);
        conversation.setTitle("新对话");
        conversation.setCreateTime(LocalDateTime.now());
        conversation.setUpdateTime(LocalDateTime.now());
        conversation.setStatus(0);
        conversationMapper.insert(conversation);
        return conversation;
    }

    @Override
    public Conversation getConversationById(String conversationId) {
        return conversationMapper.findById(conversationId);
    }

    @Override
    public List<Conversation> getConversationsByUserId(Long userId) {
        return conversationMapper.findByUserId(userId);
    }

    @Override
    public void updateConversation(Conversation conversation) {
        conversation.setUpdateTime(LocalDateTime.now());
        conversationMapper.update(conversation);
    }

    @Override
    @Transactional
    public void deleteConversation(String conversationId) {
        // 先删除所有消息
        messageMapper.deleteByConversationId(conversationId);
        // 再删除会话
        conversationMapper.delete(conversationId);
    }

    @Override
    public ConversationMessage addMessage(String conversationId, String role, String content) {
        return addMessage(conversationId, role, content, null);
    }

    @Override
    public ConversationMessage addMessage(String conversationId, String role, String content, String sources) {
        ConversationMessage message = new ConversationMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setSources(sources);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);

        // 更新会话时间
        Conversation conversation = conversationMapper.findById(conversationId);
        if (conversation != null) {
            conversation.setUpdateTime(LocalDateTime.now());
            conversationMapper.update(conversation);
        }

        return message;
    }

    @Override
    public List<ConversationMessage> getMessages(String conversationId) {
        return messageMapper.findByConversationId(conversationId);
    }

    @Override
    public List<ConversationMessage> getRecentMessages(String conversationId, int rounds) {
        // 每轮对话包含 user 和 assistant 两条消息
        int limit = rounds * 2;
        List<ConversationMessage> recentMessages = messageMapper.findRecentByConversationId(conversationId, limit);
        // 按时间正序排列
        Collections.reverse(recentMessages);
        return recentMessages;
    }

    @Override
    public void clearMessages(String conversationId) {
        messageMapper.deleteByConversationId(conversationId);
    }

    @Override
    public void updateTitle(String conversationId, String title) {
        Conversation conversation = conversationMapper.findById(conversationId);
        if (conversation != null) {
            conversation.setTitle(title);
            conversation.setUpdateTime(LocalDateTime.now());
            conversationMapper.update(conversation);
        }
    }

    @Override
    public int getMessageCount(String conversationId) {
        return messageMapper.countByConversationId(conversationId);
    }

    @Override
    public void deleteOldestRounds(String conversationId, int rounds) {
        // 每轮对话包含 user 和 assistant 两条消息
        int messageCount = rounds * 2;
        messageMapper.deleteOldestMessages(conversationId, messageCount);
    }
}