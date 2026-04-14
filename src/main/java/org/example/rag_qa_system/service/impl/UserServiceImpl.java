package org.example.rag_qa_system.service.impl;

import org.example.rag_qa_system.entity.User;
import org.example.rag_qa_system.mapper.UserMapper;
import org.example.rag_qa_system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User login(String username, String password) {
        return userMapper.findByUsernameAndPassword(username,
                DigestUtils.md5DigestAsHex(password.getBytes()));
    }

    @Override
    public void register(User user) {
        // 密码加密
        user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setStatus(0);
        userMapper.insert(user);
    }

    @Override
    public User getUserById(Long id) {
        return userMapper.findById(id);
    }

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public List<User> getAllUsers() {
        return userMapper.findAll();
    }
}