package com.minimall.minimall.controller;


import com.minimall.minimall.common.exception.BusinessException;
import com.minimall.minimall.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/hello")
public class HelloController {
    @GetMapping("/get")
    public Result<String>hello() {
        return Result.success("你好,迷你商城");
    }
    @GetMapping("/error")
    public Result<String> helloError() {
        throw new BusinessException("出异常了");
    }
}
