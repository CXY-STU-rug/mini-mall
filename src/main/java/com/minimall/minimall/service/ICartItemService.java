package com.minimall.minimall.service;

import com.minimall.minimall.entity.CartItem;
import com.baomidou.mybatisplus.extension.service.IService;
import com.minimall.minimall.vo.CartItemVO;

import java.util.List;

/**
 * <p>
 * 购物车表 服务类
 * </p>
 *
 * @author liyuq
 * @since 2026-05-18
 */
public interface ICartItemService extends IService<CartItem> {
    /** 加购（已存在则 +N，否则新增）*/
    void addToCart(Long productId, Integer quantity);
    /** 查我的购物车（带商品详情）*/
    List<CartItemVO> listMyCart();
}
