package org.example.rag_qa_system.mapper;

import org.apache.ibatis.annotations.*;
import org.example.rag_qa_system.entity.Document;

import java.util.List;

/**
 * 文档Mapper接口
 */
@Mapper
public interface DocumentMapper {

    /**
     * 根据ID查询文档
     */
    @Select("SELECT * FROM document WHERE id = #{id}")
    Document findById(Long id);

    /**
     * 根据用户ID查询文档列表
     */
    @Select("SELECT * FROM document WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Document> findByUserId(Long userId);

    /**
     * 根据知识域查询文档列表
     */
    @Select("SELECT * FROM document WHERE knowledge_domain = #{knowledgeDomain} ORDER BY create_time DESC")
    List<Document> findByKnowledgeDomain(String knowledgeDomain);

    /**
     * 根据知识域查询文档列表（支持Long类型ID）
     */
    @Select("SELECT * FROM document WHERE knowledge_domain = #{knowledgeDomain} ORDER BY create_time DESC")
    List<Document> findByKnowledgeDomainId(Long knowledgeDomain);

    /**
     * 查询所有文档
     */
    @Select("SELECT * FROM document ORDER BY create_time DESC")
    List<Document> findAll();

    /**
     * 插入文档
     */
    @Insert("INSERT INTO document(user_id, name, original_name, type, size, path, file_path, knowledge_domain, content, status, process_progress, error_message, create_time, update_time) " +
            "VALUES(#{userId}, #{name}, #{originalName}, #{type}, #{size}, #{path}, #{filePath}, #{knowledgeDomain}, #{content}, #{status}, #{processProgress}, #{errorMessage}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Document document);

    /**
     * 更新文档
     */
    @Update("UPDATE document SET name = #{name}, original_name = #{originalName}, type = #{type}, " +
            "size = #{size}, path = #{path}, file_path = #{filePath}, knowledge_domain = #{knowledgeDomain}, " +
            "content = #{content}, status = #{status}, process_progress = #{processProgress}, " +
            "error_message = #{errorMessage}, update_time = #{updateTime} WHERE id = #{id}")
    int update(Document document);

    /**
     * 删除文档（物理删除）
     */
    @Delete("DELETE FROM document WHERE id = #{id}")
    int delete(Long id);

    /**
     * 根据状态查询文档
     */
    @Select("SELECT * FROM document WHERE status = #{status} ORDER BY create_time DESC")
    List<Document> findByStatus(Integer status);

    /**
     * 根据条件查询文档
     */
    @Select("<script>" +
            "SELECT * FROM document WHERE 1=1 " +
            "<if test='userId != null'> AND user_id = #{userId}</if>" +
            "<if test='knowledgeDomain != null and knowledgeDomain != \"\"'> AND knowledge_domain = #{knowledgeDomain}</if>" +
            "<if test='status != null'> AND status = #{status}</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (name LIKE CONCAT('%', #{keyword}, '%') OR original_name LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            " ORDER BY create_time DESC" +
            "</script>")
    List<Document> findByCondition(@Param("userId") Long userId,
                                   @Param("knowledgeDomain") String knowledgeDomain,
                                   @Param("status") Integer status,
                                   @Param("keyword") String keyword);

    /**
     * 批量删除文档
     */
    @Delete("<script>" +
            "DELETE FROM document WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchDelete(@Param("ids") List<Long> ids);

    /**
     * 统计文档数量
     */
    @Select("SELECT COUNT(*) FROM document")
    int count();

    /**
     * 根据用户ID统计文档数量
     */
    @Select("SELECT COUNT(*) FROM document WHERE user_id = #{userId}")
    int countByUserId(Long userId);

    /**
     * 根据知识域统计文档数量
     */
    @Select("SELECT COUNT(*) FROM document WHERE knowledge_domain = #{knowledgeDomain}")
    int countByKnowledgeDomain(String knowledgeDomain);
}