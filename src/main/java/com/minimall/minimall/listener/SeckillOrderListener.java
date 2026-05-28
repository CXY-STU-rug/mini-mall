package com.minimall.minimall.listener;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.minimall.minimall.config.RabbitMQConfig;
import com.minimall.minimall.entity.SeckillActivity;
import com.minimall.minimall.entity.SeckillOrder;
import com.minimall.minimall.mapper.SeckillOrderMapper;
import com.minimall.minimall.service.ISeckillActivityService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * 秒杀订单异步生成消费者
 *
 * 流程：
 *   1. 收到消息 "activityId:userId"
 *   2. 解析拆分
 *   3. 幂等检查（防止 MQ 重复投递导致同一用户多条订单）
 *   4. 回查活动拿 productId / seckillPrice
 *   5. 组装 SeckillOrder 入库
 *   6. ACK / NACK
 */
@Component
public class SeckillOrderListener {

    private static final Logger log = LoggerFactory.getLogger(SeckillOrderListener.class);

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Autowired
    private ISeckillActivityService seckillActivityService;

    /**
     * 监听 seckill.queue
     * 消息体：String 类型 "activityId:userId"，例如 "1:5"
     */
    @RabbitListener(queues = RabbitMQConfig.SECKILL_QUEUE)
    public void onSeckillMessage(String msg, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.info("[MQ-Seckill] 收到秒杀消息 msg={}", msg);

        try {
            // ─── ① 解析消息 ─────────────────────────────────
            // TODO 你来写：
            //   按 ":" 拆 msg，得到 activityId 和 userId
            //   提示：
                String[] parts = msg.split(":");
                Long activityId = Long.valueOf(parts[0]);
                Long userId     = Long.valueOf(parts[1]);


            // ─── ② 幂等检查 ────────────────────────────────
            // TODO 你来写：
            //   查 seckill_order 表，看这个用户在这个活动里是否已下过单
            //   如果已存在 → log 警告 + ACK + return（不要继续）
            //   提示：
                 QueryWrapper<SeckillOrder> w = new QueryWrapper<>();
               w.eq("user_id", userId).eq("seckill_activity_id", activityId);
               Long count = seckillOrderMapper.selectCount(w);
                if (count > 0) {
                    log.warn("[MQ-Seckill] 重复消息，跳过 userId={} activityId={}", userId, activityId);
                   channel.basicAck(deliveryTag, false);
                    return;
                }


            // ─── ③ 回查活动，拿商品ID + 秒杀价 ─────────────
            // TODO 你来写：
               SeckillActivity activity = seckillActivityService.getById(activityId);
               if (activity == null) {
                   log.error("[MQ-Seckill] 活动不存在 activityId={}", activityId);
                   channel.basicNack(deliveryTag, false, false);
                   return;
               }


            // ─── ④ 组装 + 入库 ─────────────────────────────
            // TODO 你来写：
            //   生成订单号（学 OrdersServiceImpl 第 139 行的写法）
              String orderNo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                            + userId + String.format("%04d", new Random().nextInt(10000));

            SeckillOrder order = new SeckillOrder();
               order.setOrderNo(orderNo);
               order.setUserId(userId);
               order.setSeckillActivityId(activityId);
               order.setProductId(activity.getProductId());
            order.setSeckillPrice(activity.getSeckillPrice());
               order.setStatus((byte) 0);  // 0 = 待支付
               seckillOrderMapper.insert(order);
               log.info("[MQ-Seckill] 秒杀订单生成 orderNo={} userId={}", orderNo, userId);


            // ─── ⑤ ACK 确认消息消费成功 ────────────────────
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("[MQ-Seckill] 处理失败 msg={}", msg, e);
            // NACK：不重投，避免死循环（生产应配死信队列再人工处理）
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
