package com.minimall.minimall.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理员发布秒杀活动的入参
 * 注意：不含 id / status / createTime，这些后端自动生成
 */
@Data
public class SeckillActivityDTO {
    private Long productId;            // 关联哪个商品
    private BigDecimal seckillPrice;   // 秒杀价
    private Integer stock;             // 秒杀库存
    private LocalDateTime startTime;   // 活动开始时间
    private LocalDateTime endTime;     // 活动结束时间
}
