package com.minimall.minimall.dto;

import lombok.Data;

@Data
public class UserRegisterDTO {

    // 字段你来写：
    private String username;     // 用户名
    private String password;     // 密码

    // 暂时不加昵称、手机号等，先跑通最简单的
}