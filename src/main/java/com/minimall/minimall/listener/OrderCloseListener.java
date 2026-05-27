package com.minimall.minimall.listener;

import com.minimall.minimall.config.RabbitMQConfig;
import com.minimall.minimall.service.IOrdersService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 订单超时关单消费者
 * 监听 order.close.queue：消息体是 orderId，收到就调 closeOrderByMQ 关单
 */
@Component
public class OrderCloseListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCloseListener.class);

    @Autowired
    private IOrdersService ordersService;

    /**
     * 监听 close.queue
     * Spring 会按方法参数类型自动注入：
     *   - Long orderId    : 消息体（由 Jackson 反序列化）
     *   - Message message : 原始消息（含 deliveryTag 等元数据）
     *   - Channel channel : RabbitMQ 通道，用来手动 ACK/NACK
     */
    @RabbitListener(queues = RabbitMQConfig.CLOSE_QUEUE)
    public void onOrderClose(Long orderId, Message message, Channel channel) throws IOException {
        // deliveryTag：RabbitMQ 给每条投递的消息一个递增整数，ACK/NACK 时要带上
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.info("[MQ] 收到关单消息 orderId={}", orderId);

            // 调 Service 关单（内部已做"订单不存在/状态不是待付款"的幂等判断）
            ordersService.closeOrderByMQ(orderId);

            log.info("[MQ] 关单处理完成 orderId={}", orderId);

            // ⭐ 手动 ACK：告诉 MQ 这条消息处理完了，可以从队列删除
            //    参数2: multiple=false 表示只 ACK 这一条（true 是批量 ACK <= deliveryTag 的所有）
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("[MQ] 关单失败 orderId={}", orderId, e);

            // ⭐ NACK：拒绝消息
            //    参数2: multiple=false 只拒这一条
            //    参数3: requeue=false 不重新入队（避免死循环，正式项目会进死信再处理）
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
