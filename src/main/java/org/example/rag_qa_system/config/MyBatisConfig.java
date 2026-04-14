package org.example.rag_qa_system.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis配置类
 */
@Configuration
@MapperScan("org.example.rag_qa_system.mapper")
public class MyBatisConfig {
}