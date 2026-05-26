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
}
