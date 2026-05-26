package com.minimall.minimall.controller;

import com.minimall.minimall.common.result.Result;
import com.minimall.minimall.entity.Category;
import com.minimall.minimall.service.ICategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    @Autowired
    private ICategoryService categoryService;
@GetMapping
public Result<List<Category>> list() {
    List<Category> list = categoryService.list();
    return Result.success(list);
}
 @PostMapping("/add")
    public <category>Result<Category> addCategory(Category category){
    categoryService.save(category);
     return Result.success();
 }

}
