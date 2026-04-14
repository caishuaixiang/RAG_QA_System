package org.example.rag_qa_system.controller;

import org.example.rag_qa_system.entity.User;
import org.example.rag_qa_system.service.UserService;
import org.example.rag_qa_system.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result login(@RequestParam String username, @RequestParam String password) {
        try {
            User user = userService.login(username, password);
            if (user != null) {
                return Result.success(user);
            } else {
                return Result.error("用户名或密码错误");
            }
        } catch (Exception e) {
            return Result.error("登录失败: " + e.getMessage());
        }
    }

    /**
     * 用户注册
     * @param user 用户信息
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        try {
            userService.register(user);
            return Result.success("注册成功");
        } catch (Exception e) {
            return Result.error("注册失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户信息
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    public Result getUser(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            return Result.success(user);
        } catch (Exception e) {
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }
}