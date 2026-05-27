package com.minimall.minimall.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)             // ← 贴在方法上
@Retention(RetentionPolicy.RUNTIME)          // ← 运行时保留
public @interface RateLimit {    // ← 注意是 @interface

    // ← 在这写 3 个属性，记得 default 默认值
    int count() default 10;
    int seconds() default 60;
    String key() default "";
}
