package com.minimall.minimall.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateOrderDTO {
    private Long addressId;            // 选哪个收货地址
    private List<Long> cartItemIds;    // 要下单的购物车项 id 们（用户勾选）
    private String remark;             // 备注（可选）
}