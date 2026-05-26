package com.minimall.minimall.service;

import com.minimall.minimall.dto.UserLoginDTO;
import com.minimall.minimall.dto.UserRegisterDTO;
import com.minimall.minimall.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author liyuq
 * @since 2026-05-18
 */
public interface IUserService extends IService<User> {
    User register(UserRegisterDTO dto);

    String login(UserLoginDTO dto);




}
