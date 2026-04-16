package org.example.rag_qa_system.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识文档实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class Document implements Serializable {

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
     * 文档名称
     */
    private String name;

    /**
     * 文档原始文件名
     */
    private String originalName;

    /**
     * 文档类型（PDF、Word、TXT等）
     */
    private String type;

    /**
     * 文档大小（字节）
     */
    private Long size;

    /**
     * 文档路径
     */
    private String path;

    /**
     * 文档文件路径（与path相同，保持兼容性）
     */
    private String filePath;

    /**
     * 知识域分类
     */
    private String knowledgeDomain;

    /**
     * 文档内容（存储纯文本）
     */
    private String content;

    /**
     * 文档状态（0:未处理, 1:已处理, 2:处理失败）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 处理进度（0-100）
     */
    private Integer processProgress;

    /**
     * 错误信息
     */
    private String errorMessage;
}