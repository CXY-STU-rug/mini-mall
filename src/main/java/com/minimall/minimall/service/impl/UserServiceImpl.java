package com.minimall.minimall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.minimall.minimall.common.exception.BusinessException;
import com.minimall.minimall.common.util.JwtUtil;
import com.minimall.minimall.dto.UserLoginDTO;
import com.minimall.minimall.dto.UserRegisterDTO;
import com.minimall.minimall.entity.User;
import com.minimall.minimall.mapper.UserMapper;
import com.minimall.minimall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author liyuq
 * @since 2026-05-18
 */
@Service        // 注解：让 Spring 识别为 Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    // ① 定义一个静态常量加密器（类的开头）
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Override
    public User register(UserRegisterDTO dto) {

        // ② 从 dto 取出用户名和密码
        String username = dto.getUsername();   // 取用户名（提示：用 getter）
        String password = dto.getPassword();   // 取密码

        // ③ 用 Wrapper 查"用户名是否已存在"
        QueryWrapper<User> wrapper = new  QueryWrapper<>();
        wrapper.eq("username", username);   // 注意：字段名是数据库列名
        boolean exists = this.baseMapper.exists(wrapper);   // 用 BaseMapper 的"是否存在"方法

        // ④ 如果已存在，抛业务异常
        if (exists== true) {
            throw new BusinessException("用户名已存在");
        }

        // ⑤ 密码加密
        String encryptedPassword = ENCODER.encode(password);   // BCrypt 加密方法

        // ⑥ 组装 User 对象
        User user = new User();
        user.setUsername(username);
        user.setPassword(encryptedPassword);

        // ⑦ 入库（用继承自 ServiceImpl 的方法）
        this.save(user);

        // ⑧ 返回 user（id 已被自动回填）
        return user;
    }
    @Autowired
    private JwtUtil jwtUtil;        // ← 注入 JWT 工具类


    // 加在 register 方法下面
    @Override
    public String login(UserLoginDTO dto) {

        // ① 按 username 查用户
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", dto.getUsername());
        User user = this.baseMapper.selectOne(wrapper);    // ⚠️ 用 selectOne 不是 list

        // ② 没查到 → 报错
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // ③ 用 BCrypt 验证密码（matches 方法）
        boolean matches = ENCODER.matches(dto.getPassword(), user.getPassword());
        //                            ↑                ↑                  ↑
        //                       验证方法           原文密码          数据库里的乱码
        if (!matches) {
            throw new BusinessException("用户名或密码错误");
        }

        // ④ 生成 token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        return token;
    }
}