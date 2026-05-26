package com.minimall.minimall.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;       // 业务状态码
    private final String message;     // 错误信息

    // 构造方法 1：只传 message，code 默认 500
    public BusinessException(String message) {
        super(message);               // 调父类 RuntimeException 的构造
        this.code = 500;
        this.message = message;
    }

    // 构造方法 2：自定义 code 和 message
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}