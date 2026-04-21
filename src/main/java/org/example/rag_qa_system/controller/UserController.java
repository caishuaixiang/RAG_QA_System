package org.example.rag_qa_system.controller;

import org.example.rag_qa_system.entity.User;
import org.example.rag_qa_system.mapper.DocumentChunkMapper;
import org.example.rag_qa_system.mapper.DocumentMapper;
import org.example.rag_qa_system.mapper.KnowledgeBaseMapper;
import org.example.rag_qa_system.mapper.QuestionAnswerMapper;
import org.example.rag_qa_system.service.UserService;
import org.example.rag_qa_system.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private QuestionAnswerMapper questionAnswerMapper;

    @Autowired
    private DocumentChunkMapper documentChunkMapper;

    // 简单的Token存储（生产环境应使用Redis）
    private static final Map<String, Long> tokenStore = new HashMap<>();

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> loginData) {
        try {
            String username = loginData.get("username");
            String password = loginData.get("password");
            User user = userService.login(username, password);
            if (user != null) {
                // 生成Token
                String token = UUID.randomUUID().toString().replace("-", "");
                tokenStore.put(token, user.getId());

                Map<String, Object> data = new HashMap<>();
                data.put("token", token);
                data.put("user", user);
                return Result.success(data);
            } else {
                return Result.error("用户名或密码错误");
            }
        } catch (Exception e) {
            return Result.error("登录失败: " + e.getMessage());
        }
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        try {
            // 检查用户名是否已存在
            User existingUser = userService.findByUsername(user.getUsername());
            if (existingUser != null) {
                return Result.error("用户名已存在");
            }

            userService.register(user);
            return Result.success("注册成功");
        } catch (Exception e) {
            return Result.error("注册失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/{id}")
    public Result getUser(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            if (user != null) {
                // 清除密码
                user.setPassword(null);
                return Result.success(user);
            }
            return Result.error("用户不存在");
        } catch (Exception e) {
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    public Result updateUser(@PathVariable Long id, @RequestBody User user) {
        try {
            user.setId(id);
            // 不允许通过此接口修改用户名和密码
            user.setUsername(null);
            user.setPassword(null);
            userService.updateUser(user);
            User updatedUser = userService.getUserById(id);
            updatedUser.setPassword(null);
            return Result.success(updatedUser);
        } catch (Exception e) {
            return Result.error("更新用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 修改密码
     */
    @PutMapping("/{id}/password")
    public Result changePassword(@PathVariable Long id, @RequestBody Map<String, String> passwordData) {
        try {
            String oldPassword = passwordData.get("oldPassword");
            String newPassword = passwordData.get("newPassword");

            if (oldPassword == null || newPassword == null) {
                return Result.error("请提供旧密码和新密码");
            }

            boolean success = userService.changePassword(id, oldPassword, newPassword);
            if (success) {
                return Result.success("密码修改成功");
            } else {
                return Result.error("旧密码错误");
            }
        } catch (Exception e) {
            return Result.error("修改密码失败: " + e.getMessage());
        }
    }

    /**
     * 注销账号
     */
    @DeleteMapping("/{id}")
    public Result deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return Result.success("账号已注销");
        } catch (Exception e) {
            return Result.error("注销账号失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户列表（管理员）
     */
    @GetMapping("/list")
    public Result getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            List<User> users = userService.getAllUsers();
            // 清除密码
            users.forEach(u -> u.setPassword(null));

            Map<String, Object> data = new HashMap<>();
            data.put("list", users);
            data.put("total", users.size());
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result logout(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                tokenStore.remove(token);
            }
            return Result.success("退出成功");
        } catch (Exception e) {
            return Result.error("退出失败: " + e.getMessage());
        }
    }

    /**
     * 验证Token
     */
    public static Long validateToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return tokenStore.get(token);
    }

    /**
     * 获取用户统计数据
     */
    @GetMapping("/{id}/stats")
    public Result getUserStats(@PathVariable Long id) {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("knowledgeCount", knowledgeBaseMapper.countByUserId(id));
            stats.put("documentCount", documentMapper.countByUserId(id));
            stats.put("qaCount", questionAnswerMapper.countByUserId(id));
            stats.put("chunkCount", documentChunkMapper.countByUserId(id));
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("获取统计数据失败: " + e.getMessage());
        }
    }
}