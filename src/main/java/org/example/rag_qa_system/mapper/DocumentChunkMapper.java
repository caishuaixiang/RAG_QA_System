package org.example.rag_qa_system.mapper;

import org.apache.ibatis.annotations.*;
import org.example.rag_qa_system.entity.DocumentChunk;

import java.util.List;

/**
 * 文档切片Mapper接口
 */
@Mapper
public interface DocumentChunkMapper {

    /**
     * 根据ID查询文档切片
     */
    @Select("SELECT * FROM document_chunk WHERE id = #{id}")
    DocumentChunk findById(Long id);

    /**
     * 根据文档ID查询文档切片列表
     */
    @Select("SELECT * FROM document_chunk WHERE document_id = #{documentId} ORDER BY chunk_index")
    List<DocumentChunk> findByDocumentId(Long documentId);

    /**
     * 根据文档ID和标签查询文档切片
     */
    @Select("SELECT * FROM document_chunk WHERE document_id = #{documentId} AND FIND_IN_SET(#{tags}, tags) > 0")
    List<DocumentChunk> findByDocumentIdAndTags(Long documentId, String tags);

    /**
     * 插入文档切片
     */
    @Insert("INSERT INTO document_chunk(document_id, chunk_content, chunk_index, vector_id, tags, status, " +
            "start_position, end_position, page_number, section_title, paragraph_index, line_range, create_time) " +
            "VALUES(#{documentId}, #{chunkContent}, #{chunkIndex}, #{vectorId}, #{tags}, #{status}, " +
            "#{startPosition}, #{endPosition}, #{pageNumber}, #{sectionTitle}, #{paragraphIndex}, #{lineRange}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DocumentChunk documentChunk);

    /**
     * 批量插入文档切片
     */
    void batchInsert(@Param("list") List<DocumentChunk> chunks);

    /**
     * 更新文档切片
     */
    @Update("UPDATE document_chunk SET chunk_content = #{chunkContent}, vector_id = #{vectorId}, " +
            "tags = #{tags}, status = #{status}, start_position = #{startPosition}, end_position = #{endPosition}, " +
            "page_number = #{pageNumber}, section_title = #{sectionTitle}, paragraph_index = #{paragraphIndex}, " +
            "line_range = #{lineRange} WHERE id = #{id}")
    int update(DocumentChunk documentChunk);

    /**
     * 删除文档切片
     */
    @Delete("DELETE FROM document_chunk WHERE id = #{id}")
    int delete(Long id);

    /**
     * 根据文档ID删除所有切片
     */
    @Delete("DELETE FROM document_chunk WHERE document_id = #{documentId}")
    int deleteByDocumentId(Long documentId);

    /**
     * 统计切片总数
     */
    @Select("SELECT COUNT(*) FROM document_chunk")
    int count();

    /**
     * 根据用户ID统计切片数量
     */
    @Select("SELECT COUNT(*) FROM document_chunk dc " +
            "JOIN document d ON dc.document_id = d.id " +
            "WHERE d.user_id = #{userId}")
    int countByUserId(Long userId);

}