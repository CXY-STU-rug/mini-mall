package com.minimall.minimall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDetailVO {
    private Long orderId;
    private String orderNo;
    private Byte status;
    private String statusDesc;
    private BigDecimal totalAmount;
    private String receiver;
    private String phone;          // ← 详情多这个
    private String address;
    private String remark;         // ← 详情多这个
    private LocalDateTime payTime;     // ← 详情多
    private LocalDateTime shipTime;    // ← 详情多
    private LocalDateTime finishTime;  // ← 详情多
    private LocalDateTime createTime;
    private List<OrderItemVO> items;   // ← 复用 6-2 的 OrderItemVO
}