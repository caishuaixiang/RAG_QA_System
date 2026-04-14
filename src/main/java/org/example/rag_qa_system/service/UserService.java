package org.example.rag_qa_system.service;

import org.example.rag_qa_system.entity.User;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 用户信息
     */
    User login(String username, String password);

    /**
     * 用户注册
     * @param user 用户信息
     */
    void register(User user);

    /**
     * 获取用户信息
     * @param id 用户ID
     * @return 用户信息
     */
    User getUserById(Long id);

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    User findByUsername(String username);

    /**
     * 获取所有用户
     * @return 用户列表
     */
    List<User> getAllUsers();
}