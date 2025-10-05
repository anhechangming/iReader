package com.library.ireaderbackend.controller;

import com.library.ireaderbackend.Response.Result;
import com.library.ireaderbackend.Utils.JwtUtils;
import com.library.ireaderbackend.dto.UserRegisterDto;
import com.library.ireaderbackend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtils jwtUtils;

    // 注册接口
    @PostMapping("/register")
    public Result register(@RequestBody UserRegisterDto dto) {
        return userService.register(dto.getPhone(), dto.getPassword());
    }

    // 登录接口
    @PostMapping("/login")
    public Result login(@RequestBody UserRegisterDto dto) {
        return userService.login(dto.getPhone(), dto.getPassword());
    }

    // 获取当前用户信息（需要登录）
    @GetMapping("/info")
    public Result getUserInfo(HttpServletRequest request) {
        // 从请求头获取令牌
        String token = request.getHeader("Authorization");
        if (token == null || !jwtUtils.validateToken(token)) {
            return Result.fail("未登录或令牌失效");
        }
        // 解析用户ID
        Long userId = jwtUtils.getUserIdFromToken(token);
        return userService.getUserInfo(userId);
    }
}