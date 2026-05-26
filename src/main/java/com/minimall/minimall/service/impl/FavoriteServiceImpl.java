package com.minimall.minimall.service.impl;

import com.minimall.minimall.common.util.UserContext;
import com.minimall.minimall.entity.Product;
import com.minimall.minimall.service.IFavoriteService;
import com.minimall.minimall.service.IProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class FavoriteServiceImpl implements IFavoriteService {
    @Autowired
private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private IProductService productService;     // 复用商品详情接口（自带缓存）

    // 构造 key 的小函数
    private String key() {
        return "favorite:user:" + UserContext.getUserId();
    }

    @Override
    public void add(Long productId) {
        redisTemplate.opsForSet().add(key(), productId);
    }

    @Override
    public void remove(Long productId) {
        redisTemplate.opsForSet().remove(key(), productId);
    }

    @Override
    public List<Product> listMy() {
        Set<Object> productIds = redisTemplate.opsForSet().members(key());
        List<Product> result = new ArrayList<>();
        if (productIds != null) {
            for (Object pid : productIds) {
                // ⭐ 复用 getProductDetail，自带缓存——0 改动获得性能优化
                Product p = productService.getProductDetail(Long.valueOf(pid.toString()));
                if (p != null) result.add(p);
            }
        }
        return result;
    }

    @Override
    public boolean isFavorited(Long productId) {
        Boolean is = redisTemplate.opsForSet().isMember(key(), productId);
        return Boolean.TRUE.equals(is);
    }
}
