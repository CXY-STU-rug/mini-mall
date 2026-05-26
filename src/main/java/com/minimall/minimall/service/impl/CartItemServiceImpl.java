
/**
 * <p>
 * 购物车表 服务实现类
 * </p>
 *
 * @author liyuq
 * @since 2026-05-18
 */



      package com.minimall.minimall.service.impl;
  import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
  import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
  import com.minimall.minimall.common.exception.BusinessException;
  import com.minimall.minimall.common.util.UserContext;
  import com.minimall.minimall.entity.CartItem;
  import com.minimall.minimall.entity.Product;
  import com.minimall.minimall.mapper.CartItemMapper;
  import com.minimall.minimall.service.ICartItemService;
  import com.minimall.minimall.service.IProductService;
  import com.minimall.minimall.vo.CartItemVO;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.stereotype.Service;

  import java.math.BigDecimal;
  import java.util.ArrayList;
  import java.util.HashMap;
  import java.util.List;
  import java.util.Map;

@Service
    public class CartItemServiceImpl extends ServiceImpl<CartItemMapper, CartItem> implements ICartItemService {
    @Autowired
    private IProductService productService;
        @Override
        public void addToCart(Long productId, Integer quantity) {
            // 1. 校验数量
            if (quantity == null || quantity <= 0) {
                throw new BusinessException(400, "数量必须大于 0");
            }

            Long userId = UserContext.getUserId();

            // 2. 查购物车里是否已有这个商品
            QueryWrapper<CartItem> w = new QueryWrapper<>();
            w.eq("user_id", userId).eq("product_id", productId);
            CartItem existing = this.getOne(w);

            // 3. 已存在 → 数量累加；不存在 → 新建
            if (existing != null) {
                existing.setQuantity(existing.getQuantity() + quantity);
                this.updateById(existing);
            } else {
                CartItem newItem = new CartItem();
                newItem.setUserId(userId);
                newItem.setProductId(productId);
                newItem.setQuantity(quantity);
                this.save(newItem);
            }
        }
            @Override
            public List<CartItemVO> listMyCart() {
                Long userId = UserContext.getUserId();

                // ─── 步骤 1：查我的所有购物车项 ─────────────────────────
                QueryWrapper<CartItem> w = new QueryWrapper<>();
                w.eq("user_id", userId).orderByDesc("create_time");
                List<CartItem> cartItems = this.list(w);

                // 购物车空 → 直接返空列表，不用查商品
                if (cartItems.isEmpty()) {
                    return new ArrayList<>();
                }

                // ─── 步骤 2：收集所有 productId（去问 product 表要数据）──
                List<Long> productIds = new ArrayList<>();
                for (CartItem item : cartItems) {
                    productIds.add(item.getProductId());
                }
                // 此时 productIds = [1, 2, 5] 这样的列表

                // ─── 步骤 3：一次性批量查所有商品（避免循环里反复查库）──
                List<Product> products = productService.listByIds(productIds);
                // listByIds 等价于 SELECT * FROM product WHERE id IN (1,2,5)

                // ─── 步骤 4：把商品列表变成 Map<id, Product>，方便按 id 快速找 ──
                Map<Long, Product> productMap = new HashMap<>();
                for (Product p : products) {
                    productMap.put(p.getId(), p);
                }
                // 此时 productMap = {1:苹果对象, 2:香蕉对象, 5:橙子对象}

                // ─── 步骤 5：循环购物车，逐条组装 VO ───────────────────
                List<CartItemVO> result = new ArrayList<>();
                for (CartItem item : cartItems) {
                    Product product = productMap.get(item.getProductId());  // 按 id 找商品
                    if (product == null) {
                        continue;  // 商品已被删除，跳过（容错）
                    }

                    CartItemVO vo = new CartItemVO();
                    vo.setCartItemId(item.getId());
                    vo.setProductId(product.getId());
                    vo.setProductName(product.getName());
                    vo.setProductImage(product.getCoverImage());
                    vo.setPrice(product.getPrice());
                    vo.setQuantity(item.getQuantity());
                    // 小计 = 单价 × 数量（BigDecimal 必须用 .multiply，不能用 *）
                    vo.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));

                    result.add(vo);
                }

                return result;
            }


        }
