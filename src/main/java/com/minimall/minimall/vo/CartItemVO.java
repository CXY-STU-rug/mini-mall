package com.minimall.minimall.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemVO {
    private Long cartItemId;       // 购物车项 id（前端改数量/删除时用）
    private Long productId;        // 商品 id
    private String productName;    // 商品名
    private String productImage;   // 商品图
    private BigDecimal price;      // 当前单价
    private Integer quantity;      // 数量
    private BigDecimal subtotal;   // 小计 = price × quantity
}