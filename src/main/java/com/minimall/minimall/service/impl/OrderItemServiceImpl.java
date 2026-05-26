package com.minimall.minimall.service.impl;

import com.minimall.minimall.entity.OrderItem;
import com.minimall.minimall.mapper.OrderItemMapper;
import com.minimall.minimall.service.IOrderItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 订单详情表 服务实现类
 * </p>
 *
 * @author liyuq
 * @since 2026-05-18
 */
@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements IOrderItemService {

}
