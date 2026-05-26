package com.minimall.minimall.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {
    // 状态码
    private int code;
    // 消息
    private String message;
    private T data;

    public static <T> Result<T> success(int i, Object o, String s) {
        return new Result<>(200, "操作成功", null);
    }

    // 成功，带数据
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    // 失败，自定义错误信息（默认 code=500）
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    // 失败，自定义 code 和 message
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }
}


