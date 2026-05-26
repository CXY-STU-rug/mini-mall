package com.minimall.minimall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 商品表
 * </p>
 *
 * @author liyuq
 * @since 2026-05-18
 */
@Getter
@Setter
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 商品名
     */
    private String name;

    /**
     * 简短描述
     */
    private String description;

    /**
     * 详情(富文本HTML)
     */
    private String detail;

    /**
     * 价格(元)
     */
    private BigDecimal price;

    /**
     * 库存
     */
    private Integer stock;

    /**
     * 销量(冗余)
     */
    private Integer sales;

    /**
     * 封面图URL
     */
    private String coverImage;

    /**
     * 状态：0下架 1上架
     */
    private Byte status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0未删 1已删
     */
    @TableLogic
    private Byte isDeleted;
}
