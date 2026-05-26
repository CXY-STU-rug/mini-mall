
package com.minimall.minimall.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 订单明细 VO —— 给前端看一条订单里的一件商品
 *
 * 注意：这里全部用 order_item 表的快照字段，不查 product 表
 * 因为快照字段就是为了"事后不被修改影响"，没必要再去查
 */
@Data
public class OrderItemVO {
    private Long orderItemId;
    private Long productId;
    private String productName;     // 快照
    private String productImage;    // 快照
    private BigDecimal price;        // 快照
    private Integer quantity;
    private BigDecimal subtotal;
}