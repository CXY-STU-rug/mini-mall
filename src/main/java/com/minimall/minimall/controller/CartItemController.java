package com.minimall.minimall.controller;

import com.minimall.minimall.common.exception.BusinessException;
import com.minimall.minimall.common.result.Result;
import com.minimall.minimall.common.util.UserContext;
import com.minimall.minimall.dto.AddCartDTO;
import com.minimall.minimall.entity.CartItem;
import com.minimall.minimall.service.ICartItemService;
import com.minimall.minimall.vo.CartItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 购物车表 前端控制器
 * </p>
 *
 * @author liyuq
 * @since 2026-05-18
 */
@RestController
@RequestMapping("/api/cart")
public class CartItemController {
    @Autowired
    private ICartItemService cartItemService;


    @GetMapping
    public Result<List<CartItemVO>> myCart() {
        return Result.success(cartItemService.listMyCart());
    }

    // 2. 加购
    @PostMapping
    public Result<Void> add(@RequestBody AddCartDTO dto) {
        cartItemService.addToCart(dto.getProductId(), dto.getQuantity());
        return Result.success();
    }


    // 3. 改数量（≤0 自动删除）
    @PutMapping("/{id}")
    public Result<Void> updateQuantity(@PathVariable Long id,
                                       Map<String, Integer> body) {
        Integer quantity = body.get("quantity");
        CartItem item = getAndCheckOwn(id);

        if (quantity == null || quantity <= 0) {
            cartItemService.removeById(id);
        } else {
            item.setQuantity(quantity);
            cartItemService.updateById(item);
        }
        return Result.success();
    }

    // 4. 删除
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        getAndCheckOwn(id);
        cartItemService.removeById(id);
        return Result.success();
    }

    /** 越权防护：查出对象 + 校验是不是自己的 */
    private CartItem getAndCheckOwn(Long id) {
        CartItem item = cartItemService.getById(id);
        if (item == null) throw new BusinessException(404, "购物车项不存在");
        if (!item.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException(403, "无权操作");
        }
        return item;
    }

}
