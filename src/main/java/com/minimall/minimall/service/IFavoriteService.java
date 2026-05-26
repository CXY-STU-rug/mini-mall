package com.minimall.minimall.service;

import com.minimall.minimall.entity.Product;

import java.util.List;

public interface IFavoriteService {

    void add(Long productId);                 // 收藏
    void remove(Long productId);              // 取消
    List<Product> listMy();                   // 我的收藏（带商品详情）
    boolean isFavorited(Long productId);
}
