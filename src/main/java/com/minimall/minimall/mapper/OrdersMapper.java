package com.minimall.minimall.mapper;

import com.minimall.minimall.entity.Orders;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 订单主表 Mapper 接口
 * </p>
 *
 * @author liyuq
 * @since 2026-05-18
 */
@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {

}
