package com.minimall.minimall.controller;

import com.minimall.minimall.common.result.Result;
import com.minimall.minimall.dto.CreateOrderDTO;
import com.minimall.minimall.service.IOrdersService;
import com.minimall.minimall.vo.OrderDetailVO;
import com.minimall.minimall.vo.OrderListVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 订单主表 前端控制器
 * </p>
 *
 * @author liyuq
 * @since 2026-05-18
 */

    @RestController
    @RequestMapping("/api/order")
    public class OrdersController {

        @Autowired
        private IOrdersService ordersService;

        /** 创建订单 */
        @PostMapping
        public Result<Map<String, Object>> create(@RequestBody CreateOrderDTO dto) {
            return Result.success(ordersService.createOrder(dto));
        }
    @GetMapping("/my")
    public Result<List<OrderListVO>> myOrders() {
        return Result.success(ordersService.listMyOrders());
    }
    @GetMapping("/{orderId}")
    public Result<OrderDetailVO> detail(@PathVariable Long orderId) {
        return Result.success(ordersService.getOrderDetail(orderId));
    }
    @PutMapping("/{orderId}/cancel")
    public Result<Void> cancel(@PathVariable Long orderId) {
        ordersService.cancelOrder(orderId);
        return Result.success();
    }
    @PostMapping("/{orderId}/pay")
    public Result<Void> pay(@PathVariable Long orderId) {
        ordersService.payOrder(orderId);
        return Result.success();
    }
    }
