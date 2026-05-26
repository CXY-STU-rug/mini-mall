package com.minimall.minimall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 商品分类表
 * </p>
 *
 * @author liyuq
 * @since 2026-05-18
 */
@Getter
@Setter
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 分类名
     */
    private String name;

    /**
     * 图标url
     */
    private String icon;

    /**
     * 排序值，越小越靠前
     */
    private Integer sort;

    /**
     * 0=禁用,1=启用
     */
    private Byte status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Byte isDeleted;
}
