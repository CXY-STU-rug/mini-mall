package com.minimall.minimall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minimall.minimall.entity.Product;
import com.minimall.minimall.mapper.ProductMapper;
import com.minimall.minimall.service.IProductService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
 import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author liyuq
 * @since 2026-05-18
 */
@Service
@Slf4j
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService {
    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Override
    public Product getProductDetail(Long id) {
        String key = "product:detail:" + id;

        // 1. 先查 Redis
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.info("缓存命中 key={}", key);
            return (Product) cached;
        }

        // 2. 没中，查 MySQL
        log.info("缓存未命中，查 MySQL key={}", key);
        Product product = productMapper.selectById(id);
        if (product == null) {
            return null;
        }

        // 3. 存 Redis，10 分钟过期
        redisTemplate.opsForValue().set(key, product, 10, TimeUnit.MINUTES);
        return product;
    }
    @Override
    public boolean updateProduct(Product product) {
        boolean ok = updateById(product);                     // 改 MySQL
        if (ok) {
            redisTemplate.delete("product:detail:" + product.getId());   // 删缓存
            log.info("缓存已删除 key=product:detail:{}", product.getId());
        }
        return ok;
    }

    @Override
    public boolean deleteProduct(Long id) {
        boolean ok = removeById(id);                          // 删 MySQL（逻辑删）
        if (ok) {
            redisTemplate.delete("product:detail:" + id);     // 删缓存
            log.info("缓存已删除 key=product:detail:{}", id);
        }
        return ok;
    }
    @Override
    public IPage<Product> searchProducts(Integer page, Integer size,
                                         Long categoryId, String keyword,
                                         BigDecimal minPrice, BigDecimal maxPrice) {
        // ① 分页对象
        Page<Product> pageObj = new Page<>(page, size);

        // ② 动态条件
        QueryWrapper<Product> w = new QueryWrapper<>();
        if (categoryId != null) w.eq("category_id", categoryId);
        if (StringUtils.hasText(keyword)) {
            w.like("name", keyword);

            // ⭐ 在 Service 里记录热搜（Controller 看不见 Redis）
            redisTemplate.opsForZSet().incrementScore("hot:search", keyword, 1);
            redisTemplate.expire("hot:search", 24, TimeUnit.HOURS);
            log.info("记录热搜 keyword={}", keyword);
        }
        if (minPrice != null) w.ge("price", minPrice);
        if (maxPrice != null) w.le("price", maxPrice);
        w.orderByDesc("create_time");

        // ③ 调 MP 内置分页（this.page 来自父类 ServiceImpl）
        return this.page(pageObj, w);
    }

    @Override
    public List<Map<String, Object>> getHotSearch(int topN) {
        Set<ZSetOperations.TypedTuple<Object>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores("hot:search", 0, topN - 1);

        List<Map<String, Object>> result = new ArrayList<>();
        if (tuples != null) {
            for (ZSetOperations.TypedTuple<Object> t : tuples) {
                Map<String, Object> item = new HashMap<>();
                item.put("keyword", t.getValue());
                item.put("count", t.getScore());
                result.add(item);
            }
        }
        return result;
    }


}
