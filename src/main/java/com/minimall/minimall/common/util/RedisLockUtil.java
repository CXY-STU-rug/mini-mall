package com.minimall.minimall.common.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
// ↑ 导包说明：
//   StringRedisTemplate ← 专门处理「key 和 value 都是 String」的简化版 RedisTemplate
//   DefaultRedisScript  ← 封装 Lua 脚本，提交给 Redis 原子执行
//   @Component          ← 让 Spring 把这个类做成 Bean，可以 @Autowired 注入

/**
 * Redis 分布式锁工具类
 *
 * 核心两个方法：
 *   tryLock(key, seconds) → 抢锁，成功返回「锁的持有者标识」，失败返回 null
 *   unlock(key, owner)    → 释放锁（必须 Lua 原子操作，校验持有者）
 */
@Component
public class RedisLockUtil {

    private final StringRedisTemplate stringRedisTemplate;
    // ↑ Spring Boot 自动配置：spring-boot-starter-data-redis 已经帮你注册了这个 Bean

    // 构造器注入（比 @Autowired 字段注入更推荐，方便单元测试）
    public RedisLockUtil(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Lua 脚本：原子释放锁
     *   先 GET 看看是不是自己持有的锁（value 是否 == owner），是才 DEL
     *   防止 "我以为锁还在我手里，其实已经过期了被别人拿走了，我又去 DEL 把别人的删了"
     *
     * KEYS[1] = 锁的 key      （比如 lock:order:user:42）
     * ARGV[1] = 锁的持有者标识  （比如 UUID）
     */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        // static 代码块 = 类加载时执行一次，脚本对象复用
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setScriptText(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end"
        );
        UNLOCK_SCRIPT.setResultType(Long.class);
        // ↑ 返回值类型：del 成功返回 1，失败返回 0
    }

    /**
     * 尝试加锁
     * @param key            锁的 key（按业务粒度设计，如 lock:order:user:42）
     * @param expireSeconds  锁的过期时间（防死锁，秒）
     * @return 成功返回锁的「持有者标识」（释放时要用），失败返回 null
     */
    public String tryLock(String key, long expireSeconds) {
        // ① 生成本次锁的唯一标识（释放时校验，防止误删别人的锁）
        String owner = UUID.randomUUID().toString();

        // ② 执行 SET key value NX EX seconds
        //    setIfAbsent = SETNX：key 不存在才设置成功，返回 true；已存在返回 false
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, owner, expireSeconds, TimeUnit.SECONDS);

        // ③ 抢到锁就返回 owner，没抢到返回 null
        return Boolean.TRUE.equals(success) ? owner : null;
        // ↑ 用 Boolean.TRUE.equals(...) 而不是 success==true 是因为 success 可能是 null（连不上 Redis 时）
    }

    /**
     * 释放锁
     * @param key   锁的 key
     * @param owner tryLock 时拿到的标识，必须和当前 Redis 里存的一致才会真删
     * @return true=释放成功，false=锁不是你的（可能已过期、或被别人占用）
     */
    public boolean unlock(String key, String owner) {
        if (owner == null) return false;  // 当初没抢到锁就没必要解
        Long result = stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(key),   // KEYS 列表
                owner                              // ARGV 参数
        );
        return Long.valueOf(1L).equals(result);    // 1=删成功
    }
}
