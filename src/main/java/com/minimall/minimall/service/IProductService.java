package com.minimall.minimall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.minimall.minimall.entity.Product;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 商品表 服务类
 * </p>
 *
 * @author liyuq
 * @since 2026-05-18
 */
public interface IProductService extends IService<Product> {
    Product getProductDetail(Long id);
    boolean updateProduct(Product product);     // 改商品
    boolean deleteProduct(Long id);
    IPage<Product> searchProducts(Integer page, Integer size,
                                  Long categoryId, String keyword,
                                  BigDecimal minPrice, BigDecimal maxPrice);

    // 热搜 Top N
    List<Map<String, Object>> getHotSearch(int topN);
}
