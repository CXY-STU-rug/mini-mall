package com.minimall.minimall.controller;

import com.minimall.minimall.common.result.Result;
import com.minimall.minimall.common.util.UserContext;
import com.minimall.minimall.dto.UserLoginDTO;
import com.minimall.minimall.dto.UserRegisterDTO;
import com.minimall.minimall.entity.User;
import com.minimall.minimall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author liyuq
 * @since 2026-05-18
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private IUserService userService;
    @PostMapping("/register")
    public Result<User> register(@RequestBody UserRegisterDTO dto) {  // ④ 接收 JSON 参数
        User user = userService.register(dto);    // ⑤ 调 Service 注册
        return Result.success(user);
        }
    @PostMapping("/login")
    public Result<String> login(@RequestBody UserLoginDTO dto) {
        String token = userService.login(dto);
        return Result.success(token);
    }
    @GetMapping("/me")
    public Result<User> getCurrentUser() {
        Long userId = UserContext.getUserId();      // ⭐ 从 ThreadLocal 取出来！
        User user = userService.getById(userId);     // 按 id 查
        return Result.success(user);
    }

    }
