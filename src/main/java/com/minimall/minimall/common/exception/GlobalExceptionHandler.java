package com.minimall.minimall.common.exception;

import com.minimall.minimall.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常（我们自己 throw 的 BusinessException）
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());      // 用 warn 级别，不算严重错误
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 兜底：处理所有其他未捕获的异常（NullPointer、SQL异常等）
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);                       // 用 error 级别，要排查
        return Result.error("系统繁忙，请稍后再试");
    }
}

