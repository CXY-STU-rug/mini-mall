package com.minimall.minimall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 订单详情表
 * </p>
 *
 * @author liyuq
 * @since 2026-05-18
 */
@Getter
@Setter
@TableName("order_item")
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名(快照)
     */
    private String productName;

    /**
     * 商品图(快照)
     */
    private String productImage;

    /**
     * 成交单价(快照)
     */
    private BigDecimal price;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 小计=price*quantity
     */
    private BigDecimal subtotal;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
