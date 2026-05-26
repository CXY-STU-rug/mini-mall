package com.minimall.minimall.mapper;

import com.minimall.minimall.entity.Product;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 商品表 Mapper 接口
 * </p>
 *
 * @author liyuq
 * @since 2026-05-18
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    /**
     * 扣库存（防超卖核心）
     *   - stock = stock - quantity （扣库存）
     *   - sales = sales + quantity （加销量）
     *   - WHERE id = ? AND stock >= quantity   ← 关键！
     *     条件不满足时数据库返回 0 行被更新，Java 层抛"库存不足"
     *
     * @return 影响行数：1=成功，0=库存不足
     */
    @Update("UPDATE product SET stock = stock - #{quantity}, sales = sales + #{quantity} " +
            "WHERE id = #{productId} AND stock >= #{quantity}")
    int deductStock(@Param("productId") Long productId,
                    @Param("quantity") Integer quantity);


    @Update("UPDATE product SET stock = stock + #{quantity}, sales = sales - #{quantity} " +
            "WHERE id = #{productId}")
    int restoreStock(@Param("productId") Long productId,
                     @Param("quantity") Integer quantity);
}
