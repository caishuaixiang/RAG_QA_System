-- RAG智能问答系统数据库建表脚本
-- 执行时间：2026-04-15

-- 创建数据库
CREATE DATABASE IF NOT EXISTS rag_qa_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE rag_qa_system;

-- ----------------------------
-- 1. 用户表
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
                        `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                        `username` VARCHAR(50) NOT NULL COMMENT '用户名',
                        `password` VARCHAR(100) NOT NULL COMMENT '密码（MD5加密）',
                        `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
                        `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
                        `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
                        `role` INT DEFAULT 0 COMMENT '用户角色（0:普通用户, 1:管理员）',
                        `status` INT DEFAULT 0 COMMENT '用户状态（0:正常, 1:禁用）',
                        `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `uk_username` (`username`),
                        KEY `idx_email` (`email`),
                        KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 插入默认管理员账号（密码：admin123）
INSERT INTO `user` (`username`, `password`, `nickname`, `role`, `status`)
VALUES ('admin', '0192023a7bbd73250516f069df18b500', '管理员', 1, 0);

-- ----------------------------
-- 2. 知识库表
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE `knowledge_base` (
                                  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                  `user_id` BIGINT NOT NULL COMMENT '用户ID',
                                  `name` VARCHAR(100) NOT NULL COMMENT '知识库名称',
                                  `description` VARCHAR(500) DEFAULT NULL COMMENT '知识库描述',
                                  `category` VARCHAR(50) DEFAULT NULL COMMENT '知识库分类',
                                  `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签（逗号分隔）',
                                  `is_public` INT DEFAULT 0 COMMENT '是否公开（0:私有, 1:公开）',
                                  `status` INT DEFAULT 1 COMMENT '状态（0:禁用, 1:启用）',
                                  `document_count` INT DEFAULT 0 COMMENT '文档数量',
                                  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_user_id` (`user_id`),
                                  KEY `idx_status` (`status`),
                                  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

-- ----------------------------
-- 3. 文档表
-- ----------------------------
DROP TABLE IF EXISTS `document`;
CREATE TABLE `document` (
                            `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                            `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
                            `name` VARCHAR(255) NOT NULL COMMENT '文档名称',
                            `original_name` VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
                            `type` VARCHAR(50) DEFAULT NULL COMMENT '文档类型（PDF、Word、TXT等）',
                            `size` BIGINT DEFAULT NULL COMMENT '文档大小（字节）',
                            `path` VARCHAR(500) DEFAULT NULL COMMENT '文档路径',
                            `file_path` VARCHAR(500) DEFAULT NULL COMMENT '文档文件路径',
                            `knowledge_domain` VARCHAR(100) DEFAULT NULL COMMENT '知识域分类（对应知识库ID）',
                            `content` LONGTEXT COMMENT '文档内容（纯文本）',
                            `status` INT DEFAULT 0 COMMENT '文档状态（0:未处理, 1:已处理, 2:处理失败）',
                            `process_progress` INT DEFAULT 0 COMMENT '处理进度（0-100）',
                            `error_message` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
                            `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (`id`),
                            KEY `idx_user_id` (`user_id`),
                            KEY `idx_knowledge_domain` (`knowledge_domain`),
                            KEY `idx_status` (`status`),
                            KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';

-- ----------------------------
-- 4. 文档切片表
-- ----------------------------
DROP TABLE IF EXISTS `document_chunk`;
CREATE TABLE `document_chunk` (
                                  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                  `document_id` BIGINT NOT NULL COMMENT '文档ID',
                                  `chunk_content` TEXT NOT NULL COMMENT '切片内容',
                                  `chunk_index` INT DEFAULT NULL COMMENT '切片序号',
                                  `vector_id` VARCHAR(255) DEFAULT NULL COMMENT '向量ID',
                                  `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签（逗号分隔）',
                                  `status` INT DEFAULT 0 COMMENT '切片状态（0:未处理, 1:已处理, 2:处理失败）',
                                  `start_position` INT DEFAULT NULL COMMENT '切片在原文中的起始位置（字符索引）',
                                  `end_position` INT DEFAULT NULL COMMENT '切片在原文中的结束位置（字符索引）',
                                  `page_number` INT DEFAULT NULL COMMENT '页码（对于PDF等分页文档）',
                                  `section_title` VARCHAR(500) DEFAULT NULL COMMENT '章节标题',
                                  `paragraph_index` INT DEFAULT NULL COMMENT '段落序号',
                                  `line_range` VARCHAR(50) DEFAULT NULL COMMENT '行号范围（格式：startLine-endLine）',
                                  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_document_id` (`document_id`),
                                  KEY `idx_document_chunk_position` (`document_id`, `start_position`, `end_position`),
                                  KEY `idx_document_chunk_page` (`document_id`, `page_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档切片表';
-- ----------------------------
-- 5. 问答记录表
-- ----------------------------
DROP TABLE IF EXISTS `question_answer`;
CREATE TABLE `question_answer` (
                                   `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                   `user_id` BIGINT NOT NULL COMMENT '用户ID',
                                   `question` TEXT NOT NULL COMMENT '问题内容',
                                   `answer` LONGTEXT COMMENT '答案内容',
                                   `source` TEXT COMMENT '答案来源（JSON格式）',
                                   `vector_ids` VARCHAR(500) DEFAULT NULL COMMENT '向量ID列表（逗号分隔）',
                                   `status` INT DEFAULT 0 COMMENT '问答状态（0:正常, 1:已删除）',
                                   `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   PRIMARY KEY (`id`),
                                   KEY `idx_user_id` (`user_id`),
                                   KEY `idx_create_time` (`create_time`),
                                   FULLTEXT KEY `ft_question` (`question`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问答记录表';

-- ----------------------------
-- 6. 系统配置表（可选）
-- ----------------------------
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
                                 `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                 `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
                                 `config_value` VARCHAR(500) DEFAULT NULL COMMENT '配置值',
                                 `config_desc` VARCHAR(255) DEFAULT NULL COMMENT '配置描述',
                                 `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 插入默认配置
INSERT INTO `system_config` (`config_key`, `config_value`, `config_desc`) VALUES
                                                                              ('chunk_max_length', '500', '切片最大长度（字符数）'),
                                                                              ('chunk_overlap', '50', '切片重叠长度（字符数）'),
                                                                              ('embedding_model', 'text-embedding-ada-002', '向量嵌入模型'),
                                                                              ('llm_model', 'chatglm', '大语言模型'),
                                                                              ('top_k', '3', '检索返回的文档数量');

-- ----------------------------
-- 创建视图（可选）
-- ----------------------------

-- 文档统计视图
CREATE OR REPLACE VIEW `v_document_stats` AS
SELECT
    d.knowledge_domain,
    COUNT(*) as document_count,
    SUM(d.size) as total_size,
    COUNT(CASE WHEN d.status = 1 THEN 1 END) as processed_count,
    COUNT(CASE WHEN d.status = 0 THEN 1 END) as pending_count,
    COUNT(CASE WHEN d.status = 2 THEN 1 END) as failed_count
FROM document d
GROUP BY d.knowledge_domain;

-- 用户统计视图
CREATE OR REPLACE VIEW `v_user_stats` AS
SELECT
    u.id as user_id,
    u.username,
    COUNT(DISTINCT kb.id) as knowledge_base_count,
    COUNT(DISTINCT d.id) as document_count,
    COUNT(DISTINCT qa.id) as qa_count
FROM user u
         LEFT JOIN knowledge_base kb ON u.id = kb.user_id
         LEFT JOIN document d ON u.id = d.user_id
         LEFT JOIN question_answer qa ON u.id = qa.user_id
GROUP BY u.id, u.username;