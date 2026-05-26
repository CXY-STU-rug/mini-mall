package com.minimall.minimall.controller;

import com.minimall.minimall.common.result.Result;
import com.minimall.minimall.entity.Product;
import com.minimall.minimall.service.IFavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {

    @Autowired
    private IFavoriteService favoriteService;

    @PostMapping("/{productId}")
    public Result<Void> add(@PathVariable Long productId) {
        favoriteService.add(productId);
        return Result.success();
    }

    @DeleteMapping("/{productId}")
    public Result<Void> remove(@PathVariable Long productId) {
        favoriteService.remove(productId);
        return Result.success();
    }

    @GetMapping("/my")
    public Result<List<Product>> listMy() {
        return Result.success(favoriteService.listMy());
    }

    @GetMapping("/{productId}/exists")
    public Result<Boolean> exists(@PathVariable Long productId) {
        return Result.success(favoriteService.isFavorited(productId));
    }
}