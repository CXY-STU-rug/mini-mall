package com.minimall.minimall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * Lua 脚本统一配置
 * 在这里注册所有 Lua 脚本为 Spring Bean，启动时加载一次
 * 之后业务类直接 @Autowired 注入使用
 */
@Configuration
public class LuaScriptConfig {

    /**
     * 秒杀预扣库存脚本
     * 文件位置：src/main/resources/lua/seckill_stock.lua
     * 返回类型：Long（脚本返回数字 1/0/-1/-2）
     */
    @Bean
    public DefaultRedisScript<Long> seckillStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("lua/seckill_stock.lua"))
        );
        script.setResultType(Long.class);
        return script;
    }
}
