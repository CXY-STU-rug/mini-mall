package com.minimall.minimall.aspect;

import com.minimall.minimall.common.annotation.RateLimit;
import com.minimall.minimall.common.exception.BusinessException;
import com.minimall.minimall.common.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;
// ↑ 导包说明：
//   @Aspect / @Around / ProceedingJoinPoint → AspectJ 提供的切面注解
//   RequestContextHolder → Spring 在每个请求线程里塞的当前 HTTP 上下文
//   StringRedisTemplate → 纯字符串 Redis 模板（计数场景用 String 即可）
//   jakarta.servlet → Spring Boot 3 用 jakarta 而不是 javax

/**
 * 限流切面：拦截 @RateLimit 注解，用 Redis INCR 实现固定窗口算法
 */
@Aspect          // ← 标记这是一个 AOP 切面
@Component       // ← 让 Spring 把它做成 Bean
public class RateLimitAspect {

    private final StringRedisTemplate stringRedisTemplate;

    // 构造器注入（比 @Autowired 字段注入更推荐）
    public RateLimitAspect(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 切点：拦截所有带 @RateLimit 的方法
     *
     * @Around("@annotation(rateLimit)")
     *   → 拦截带 @RateLimit 注解的方法
     *   → 同时把注解对象作为参数注入（必须叫 rateLimit，名字要和括号里的一致）
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {

        // ─── ① 拼出 Redis 的 key ───────────────────────
        // 根据注解的 key 字段决定限流维度：ip / user / global
        String dimension;
        switch (rateLimit.key()) {
            case "ip":
                dimension = "ip:" + getClientIp();
                break;
            case "user":
                dimension = "user:" + UserContext.getUserId();
                break;
            default:
                dimension = "global";   // 全局限流（所有人共用一个计数器）
        }

        // 拼接：rate:类名#方法名:维度
        // 例如：rate:UserController#login:ip:192.168.1.5
        String methodName = pjp.getSignature().getDeclaringType().getSimpleName()
                + "#" + pjp.getSignature().getName();
        String redisKey = "rate:" + methodName + ":" + dimension;

        // ─── ② INCR 计数 ─────────────────────────────
        // INCR：如果 key 不存在则创建并设为 0，然后 +1，返回新值
        Long count = stringRedisTemplate.opsForValue().increment(redisKey);

        // ─── ③ 第一次访问时设置过期时间 ───────────────
        // count == 1 说明 key 是这次新建的，要给它设过期时间
        // 如果不设，key 会永久存在，第二个时间窗口的计数永远超
        if (count != null && count == 1) {
            stringRedisTemplate.expire(redisKey, rateLimit.seconds(), TimeUnit.SECONDS);
        }

        // ─── ④ 超过阈值则拦截 ────────────────────────
        if (count != null && count > rateLimit.count()) {
            throw new BusinessException(429,
                    "操作太频繁，请稍后再试（" + rateLimit.seconds() + "s 内最多 " + rateLimit.count() + " 次）");
        }

        // ─── ⑤ 放行：执行原方法 ──────────────────────
        return pjp.proceed();
        // ↑ 关键！不调 proceed() 就等于把方法吞了，业务永远不执行
    }

    /**
     * 拿到当前请求的客户端 IP
     * 注意：生产环境过 Nginx 反代后要读 X-Forwarded-For 头
     */
    private String getClientIp() {
        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest();

        // 优先看反代头（Nginx 会写这个），没有就用 remote addr
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能是 "ip1, ip2, ip3" 多层代理，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }
}
