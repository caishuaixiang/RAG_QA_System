package org.example.rag_qa_system.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话消息实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class ConversationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 消息角色（user/assistant）
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 答案来源信息（JSON格式，仅assistant消息有）
     */
    private String sources;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}