package org.example.rag_qa_system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.rag_qa_system.entity.User;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Options;

import java.util.List;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper {

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);

    /**
     * 根据用户名和密码查询用户
     */
    @Select("SELECT * FROM user WHERE username = #{username} AND password = #{password} AND status = 0")
    User findByUsernameAndPassword(String username, String password);

    /**
     * 根据ID查询用户
     */
    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(Long id);

    /**
     * 查询所有用户
     */
    @Select("SELECT * FROM user")
    List<User> findAll();

    /**
     * 插入用户
     */
    @Insert("INSERT INTO user(username, password, nickname, email, phone, status, create_time, update_time) " +
            "VALUES(#{username}, #{password}, #{nickname}, #{email}, #{phone}, #{status}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    /**
     * 更新用户
     */
    @Update("UPDATE user SET username = #{username}, password = #{password}, nickname = #{nickname}, " +
            "email = #{email}, phone = #{phone}, status = #{status}, update_time = #{updateTime} " +
            "WHERE id = #{id}")
    int update(User user);

    /**
     * 删除用户
     */
    @Delete("DELETE FROM user WHERE id = #{id}")
    int delete(Long id);
}