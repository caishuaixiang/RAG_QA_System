package org.example.rag_qa_system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.rag_qa_system.entity.Document;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

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
     * 插入文档
     */
    @Insert("INSERT INTO document(user_id, title, content, file_path, file_type, file_size, status, create_time, update_time) " +
            "VALUES(#{userId}, #{title}, #{content}, #{filePath}, #{fileType}, #{fileSize}, #{status}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Document document);

    /**
     * 更新文档
     */
    @Update("UPDATE document SET title = #{title}, content = #{content}, file_path = #{filePath}, " +
            "file_type = #{fileType}, file_size = #{fileSize}, status = #{status}, update_time = #{updateTime} " +
            "WHERE id = #{id}")
    int update(Document document);

    /**
     * 删除文档
     */
    @Update("UPDATE document SET status = 1 WHERE id = #{id}")
    int delete(Long id);

    /**
     * 根据状态查询文档
     */
    @Select("SELECT * FROM document WHERE status = #{status} ORDER BY create_time DESC")
    List<Document> findByStatus(Integer status);
}