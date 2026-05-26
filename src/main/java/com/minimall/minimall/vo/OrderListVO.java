package com.minimall.minimall.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单列表 VO —— 给"我的订单"页面用
 * 一个订单 + 它的所有明细，组合在一起
 */
@Data
public class OrderListVO {
    private Long orderId;
    private String orderNo;
    private Byte status;              // 原始状态值（0-4）
    private String statusDesc;        // 中文描述："待付款"等，前端直接显示
    private BigDecimal totalAmount;
    private String receiver;
    private String address;
    private LocalDateTime createTime;
    private List<OrderItemVO> items;  // ← 关键！一对多
}