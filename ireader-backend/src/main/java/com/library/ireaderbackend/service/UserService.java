package com.library.ireaderbackend.service;


import com.library.ireaderbackend.Response.Result;
import com.library.ireaderbackend.Utils.JwtUtils;
import com.library.ireaderbackend.entity.LoginResponse;
import com.library.ireaderbackend.entity.User;
import com.library.ireaderbackend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtUtils jwtUtils;

    // 注册
    public Result register(String phone, String password) {
        // 1. 校验手机号是否已注册（调用 mapper 的 selectByPhone）
        User existUser = userMapper.selectByPhone(phone);
        if (existUser != null) {
            return Result.fail("手机号已注册");
        }
        // 2. 密码加密（MD5）
        String encryptedPwd = DigestUtils.md5DigestAsHex(password.getBytes());
        // 3. 生成默认昵称
        String nickname = "用户" + phone.substring(phone.length() - 4);
        // 4. 保存用户（调用 mapper 的 insert）
        User user = new User();
        user.setPhone(phone);
        user.setPassword(encryptedPwd);
        user.setNickname(nickname);
        userMapper.insert(user); // MyBatis 原生 insert 方法
        return Result.success(user.getId());
    }

    // 登录（逻辑同上，调用 userMapper.selectByPhone 校验）
    public Result login(String phone, String password) {
        User user = userMapper.selectByPhone(phone);
        if (user == null) {
            return Result.fail("手机号未注册");
        }
        String encryptedPwd = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!encryptedPwd.equals(user.getPassword())) {
            return Result.fail("密码错误");
        }
        String token = jwtUtils.generateToken(user.getId());
        LoginResponse response = new LoginResponse(user, token);
        return Result.success(response);
    }

    // 获取用户信息（调用 mapper 的 selectById）
    public Result getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        user.setPassword(null); // 脱敏
        return Result.success(user);
    }
}