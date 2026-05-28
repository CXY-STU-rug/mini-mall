package com.minimall.minimall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.Date;
@Data
@TableName("seckill_order")
public class SeckillOrder {
    @TableId(value = "id",type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private Long userId;
    private Long seckillActivityId;
    private Long productId;
    private Byte status;
    private BigDecimal seckillPrice;
private LocalDateTime payTime;
private LocalDateTime createTime;
private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;
}
