package org.example.rag_qa_system.mapper;

import org.apache.ibatis.annotations.*;
import org.example.rag_qa_system.entity.KnowledgeBase;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库Mapper接口
 */
@Mapper
public interface KnowledgeBaseMapper {

    /**
     * 插入知识库
     */
    @Insert("INSERT INTO knowledge_base(user_id, name, description, category, tags, is_public, status, create_time, update_time) " +
            "VALUES(#{userId}, #{name}, #{description}, #{category}, #{tags}, #{isPublic}, #{status}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeBase knowledgeBase);

    /**
     * 根据ID查询知识库
     */
    @Select("SELECT * FROM knowledge_base WHERE id = #{id}")
    KnowledgeBase findById(Long id);

    /**
     * 查询所有知识库
     */
    @Select("SELECT * FROM knowledge_base ORDER BY create_time DESC")
    List<KnowledgeBase> findAll();

    /**
     * 根据用户ID查询知识库
     */
    @Select("SELECT * FROM knowledge_base WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<KnowledgeBase> findByUserId(Long userId);

    /**
     * 根据状态查询知识库
     */
    @Select("SELECT * FROM knowledge_base WHERE status = #{status} ORDER BY create_time DESC")
    List<KnowledgeBase> findByStatus(Integer status);

    /**
     * 根据条件查询知识库
     */
    @Select("<script>" +
            "SELECT * FROM knowledge_base WHERE 1=1 " +
            "<if test='userId != null'> AND user_id = #{userId}</if>" +
            "<if test='name != null and name != \"\"'> AND name LIKE CONCAT('%', #{name}, '%')</if>" +
            "<if test='status != null'> AND status = #{status}</if>" +
            "<if test='isPublic != null'> AND is_public = #{isPublic}</if>" +
            " ORDER BY create_time DESC" +
            "</script>")
    List<KnowledgeBase> findByCondition(@Param("userId") Long userId,
                                        @Param("name") String name,
                                        @Param("status") Integer status,
                                        @Param("isPublic") Integer isPublic);

    /**
     * 更新知识库
     */
    @Update("UPDATE knowledge_base SET name = #{name}, description = #{description}, " +
            "category = #{category}, tags = #{tags}, is_public = #{isPublic}, " +
            "status = #{status}, update_time = #{updateTime} WHERE id = #{id}")
    int update(KnowledgeBase knowledgeBase);

    /**
     * 更新文档数量
     */
    @Update("UPDATE knowledge_base SET document_count = #{documentCount}, update_time = #{updateTime} WHERE id = #{id}")
    int updateDocumentCount(@Param("id") Long id,
                            @Param("documentCount") Integer documentCount,
                            @Param("updateTime") LocalDateTime updateTime);

    /**
     * 删除知识库
     */
    @Delete("DELETE FROM knowledge_base WHERE id = #{id}")
    int delete(Long id);

    /**
     * 统计知识库数量
     */
    @Select("SELECT COUNT(*) FROM knowledge_base")
    int count();

    /**
     * 根据用户ID统计知识库数量
     */
    @Select("SELECT COUNT(*) FROM knowledge_base WHERE user_id = #{userId}")
    int countByUserId(Long userId);
}