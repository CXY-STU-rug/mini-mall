package com.minimall.minimall.service;

import com.minimall.minimall.dto.CreateOrderDTO;
import com.minimall.minimall.entity.Orders;
import com.baomidou.mybatisplus.extension.service.IService;
import com.minimall.minimall.vo.OrderDetailVO;
import com.minimall.minimall.vo.OrderListVO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 订单主表 服务类
 * </p>
 *
 * @author liyuq
 * @since 2026-05-18
 */
public interface IOrdersService extends IService<Orders> {
    Map<String, Object> createOrder(CreateOrderDTO dto);
    int closeTimeoutOrders();
    void cancelOrder(Long orderId);
    OrderDetailVO getOrderDetail(Long orderId);
    List<OrderListVO> listMyOrders();
    void payOrder(Long orderId);

    /**
     * MQ 消费者专用：根据订单 ID 关单（不校验登录用户、自带幂等）
     * 跟 cancelOrder 的区别：不读 UserContext（消费者线程没有 HTTP 上下文）
     */
    void closeOrderByMQ(Long orderId);
}
