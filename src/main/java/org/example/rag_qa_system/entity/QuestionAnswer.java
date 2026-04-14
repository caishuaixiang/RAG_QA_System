package org.example.rag_qa_system.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 问答记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class QuestionAnswer implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 问题内容
     */
    private String question;

    /**
     * 答案内容
     */
    private String answer;

    /**
     * 答案来源
     */
    private String source;

    /**
     * 向量ID列表（逗号分隔）
     */
    private String vectorIds;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 问答状态（0:正常, 1:已删除）
     */
    private Integer status;
}