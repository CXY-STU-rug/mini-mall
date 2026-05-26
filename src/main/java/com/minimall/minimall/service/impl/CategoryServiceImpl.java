package com.minimall.minimall.service.impl;

import com.minimall.minimall.entity.Category;
import com.minimall.minimall.mapper.CategoryMapper;
import com.minimall.minimall.service.ICategoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 商品分类表 服务实现类
 * </p>
 *
 * @author liyuq
 * @since 2026-05-18
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements ICategoryService {

}
