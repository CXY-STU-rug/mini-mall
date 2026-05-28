package com.minimall.minimall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动展示 VO（给前端）
 * 比 SeckillActivity 实体多了商品名/图片/原价，前端可以直接渲染
 * 比实体少了 isDeleted/updateTime 等内部字段
 */
@Data
public class SeckillActivityVO {
    private Long id;                    // 活动 id
    private Long productId;
    private String productName;         // ⭐ 商品名（从 product 表带出）
    private String productImage;        // ⭐ 商品图（从 product 表带出）
    private BigDecimal originalPrice;   // ⭐ 商品原价（前端要划掉显示）
    private BigDecimal seckillPrice;    // 秒杀价
    private Integer stock;              // 秒杀库存
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Byte status;                // 0待开始 1进行中 2已结束
    private String statusDesc;          // 状态中文描述
}
