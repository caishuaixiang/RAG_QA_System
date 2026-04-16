-- 为document_chunk表添加位置溯源字段
-- 执行时间：2026-04-15
-- 说明：增强RAG问答系统的答案溯源能力

ALTER TABLE document_chunk
    ADD COLUMN start_position INT DEFAULT NULL COMMENT '切片在原文中的起始位置（字符索引）' AFTER status,
ADD COLUMN end_position INT DEFAULT NULL COMMENT '切片在原文中的结束位置（字符索引）' AFTER start_position,
ADD COLUMN page_number INT DEFAULT NULL COMMENT '页码（对于PDF等分页文档）' AFTER end_position,
ADD COLUMN section_title VARCHAR(500) DEFAULT NULL COMMENT '章节标题' AFTER page_number,
ADD COLUMN paragraph_index INT DEFAULT NULL COMMENT '段落序号' AFTER section_title,
ADD COLUMN line_range VARCHAR(50) DEFAULT NULL COMMENT '行号范围（格式：startLine-endLine）' AFTER paragraph_index;

-- 添加索引以支持按位置查询
CREATE INDEX idx_document_chunk_position ON document_chunk(document_id, start_position, end_position);
CREATE INDEX idx_document_chunk_page ON document_chunk(document_id, page_number);