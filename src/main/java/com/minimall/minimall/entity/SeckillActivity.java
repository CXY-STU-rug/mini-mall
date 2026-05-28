package com.minimall.minimall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data                                  // ← Lombok 自动生成 getter/setter
@TableName("seckill_activity")
public class SeckillActivity {

@TableId(value = "id",type =IdType.AUTO  )
    private Long id;
private Long productId;
private BigDecimal seckillPrice;

private Integer stock;
private Byte status;
private LocalDateTime createTime;
private LocalDateTime startTime;

private LocalDateTime endTime;
}
