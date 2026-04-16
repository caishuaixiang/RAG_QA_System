package org.example.rag_qa_system.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文档切片实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class DocumentChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 切片内容
     */
    private String chunkContent;

    /**
     * 切片序号
     */
    private Integer chunkIndex;

    /**
     * 向量ID
     */
    private String vectorId;

    /**
     * 标签（逗号分隔）
     */
    private String tags;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 切片状态（0:未处理, 1:已处理, 2:处理失败）
     */
    private Integer status;

    /**
     * 切片在原文中的起始位置（字符索引）
     */
    private Integer startPosition;

    /**
     * 切片在原文中的结束位置（字符索引）
     */
    private Integer endPosition;

    /**
     * 页码（对于PDF等分页文档）
     */
    private Integer pageNumber;

    /**
     * 章节标题（如果文档有章节结构）
     */
    private String sectionTitle;

    /**
     * 段落序号（在文档中的段落位置）
     */
    private Integer paragraphIndex;

    /**
     * 行号范围（格式：startLine-endLine）
     */
    private String lineRange;
}