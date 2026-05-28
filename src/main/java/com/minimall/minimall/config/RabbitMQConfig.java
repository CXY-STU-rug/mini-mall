package com.minimall.minimall.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 配置：订单延迟关单
 * 流程：createOrder → delay.exchange → delay.queue（TTL 到期）
 *      → close.exchange（DLX）→ close.queue → OrderCloseListener 消费
 */
@Configuration
public class RabbitMQConfig {

    // ========== 常量：队列名/交换机名/路由键 ==========
    public static final String DELAY_EXCHANGE = "order.delay.exchange";
    public static final String DELAY_QUEUE    = "order.delay.queue";
    public static final String DELAY_ROUTING_KEY = "delay";

    public static final String CLOSE_EXCHANGE = "order.close.exchange";
    public static final String CLOSE_QUEUE    = "order.close.queue";
    public static final String CLOSE_ROUTING_KEY = "close";

    // ========== 秒杀异步下单（普通队列，不要 TTL/DLX）==========
    public static final String SECKILL_EXCHANGE = "seckill.exchange";
    public static final String SECKILL_QUEUE    = "seckill.queue";
    public static final String SECKILL_ROUTING_KEY = "seckill";

    // ========== 1) 延迟交换机（Direct 类型）==========
    @Bean
    public DirectExchange delayExchange() {
        return new DirectExchange(DELAY_EXCHANGE, true, false);
        // 参数：name, durable(持久化), autoDelete(无队列绑定时自动删)
    }

    // ========== 2) 延迟队列（核心：配 TTL + DLX）==========
    @Bean
    public Queue delayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 30000);                       // ⏱ 30 秒（测试用，上线改
        args.put("x-dead-letter-exchange", CLOSE_EXCHANGE);     // 💀 死信去哪个交换机
        args.put("x-dead-letter-routing-key", CLOSE_ROUTING_KEY); // 💀 死信用什么 routingKey
        return new Queue(DELAY_QUEUE, true, false, false, args);
        // 参数：name, durable, exclusive(独占), autoDelete, arguments
    }

    // ========== 3) 延迟队列绑定到延迟交换机 ==========
    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue())
                .to(delayExchange())
                .with(DELAY_ROUTING_KEY);
    }

    // ========== 4) 关单交换机（DLX 用）==========
    @Bean
    public DirectExchange closeExchange() {
        return new DirectExchange(CLOSE_EXCHANGE, true, false);
    }

    // ========== 5) 关单队列（消费者监听这个）==========
    @Bean
    public Queue closeQueue() {
        return new Queue(CLOSE_QUEUE, true);
    }

    // ========== 6) 关单队列绑定到关单交换机 ==========
    @Bean
    public Binding closeBinding() {
        return BindingBuilder.bind(closeQueue())
                .to(closeExchange())
                .with(CLOSE_ROUTING_KEY);
    }

    // ========== 7) 秒杀异步下单交换机 ==========
    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange(SECKILL_EXCHANGE, true, false);
    }

    // ========== 8) 秒杀异步下单队列 ==========
    @Bean
    public Queue seckillQueue() {
        return new Queue(SECKILL_QUEUE, true);
    }

    // ========== 9) 秒杀队列绑定 ==========
    @Bean
    public Binding seckillBinding() {
        return BindingBuilder.bind(seckillQueue())
                .to(seckillExchange())
                .with(SECKILL_ROUTING_KEY);
    }
}