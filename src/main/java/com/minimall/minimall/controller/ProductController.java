package com.minimall.minimall.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minimall.minimall.common.annotation.RateLimit;
import com.minimall.minimall.common.result.Result;
import com.minimall.minimall.entity.Product;
import com.minimall.minimall.service.IProductService;
import com.minimall.minimall.service.impl.ProductServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 商品表 前端控制器
 * </p>
 *
 * @author liyuq
 * @since 2026-05-18
 */
@RestController
@RequestMapping("api/product")
public class ProductController {

    @Autowired
    private  IProductService productService;

    // ========== 1. 列表（分页 + 筛选 + 搜索）==========
    @GetMapping
    public Result<IPage<Product>> list(
            @RequestParam(defaultValue = "1")  Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice
    ) {
        return Result.success(
                productService.searchProducts(page, size, categoryId, keyword, minPrice, maxPrice)
        );
    }



    // ========== 2. 详情 ==========
    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.success(productService.getProductDetail(id));
    }

    // ========== 3. 上架 ==========
    @PostMapping
    public Result<Product> create(Product product) {
        productService.save(product);
        return Result.success(product);
    }

    // ========== 4. 修改 ==========
    @PutMapping("/{id}")
    public Result<Product> update(@PathVariable Long id, @RequestBody Product product) {  // ← 加 @RequestBody
        product.setId(id);
        productService.updateProduct(product);                // ← 调你新写的
        return Result.success(product);
    }
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {

        productService.deleteProduct(id);                     // ← 改这里
        return Result.success();
    }
    @RateLimit(count = 10, seconds = 60, key = "user")
    @GetMapping("/hot-search")
    public Result<List<Map<String, Object>>> hotSearch() {
        return Result.success(productService.getHotSearch(10));
    }

    }



