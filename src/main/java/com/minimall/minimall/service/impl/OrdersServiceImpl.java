package com.minimall.minimall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.minimall.minimall.common.common.constant.OrderStatus;
import com.minimall.minimall.common.exception.BusinessException;
import com.minimall.minimall.common.util.RedisLockUtil;
import com.minimall.minimall.common.util.UserContext;
import com.minimall.minimall.config.RabbitMQConfig;
import com.minimall.minimall.dto.CreateOrderDTO;
import com.minimall.minimall.entity.*;
import com.minimall.minimall.mapper.OrderItemMapper;
import com.minimall.minimall.mapper.OrdersMapper;
import com.minimall.minimall.mapper.ProductMapper;
import com.minimall.minimall.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.minimall.minimall.vo.OrderDetailVO;
import com.minimall.minimall.vo.OrderItemVO;
import com.minimall.minimall.vo.OrderListVO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * <p>
 * 订单主表 服务实现类
 * </p>
 *
 * @author liyuq
 * @since 2026-05-18
 */
@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements IOrdersService {
    @Autowired
    private IAddressService addressService;       // 查地址
    @Autowired
    private ICartItemService cartItemService;     // 查购物车 / 删购物车
    @Autowired
    private IProductService productService;       // 查商品
    @Autowired
    private ProductMapper productMapper;          // 直接调自定义 SQL 扣库存
    @Autowired
    private IOrderItemService orderItemService;
    @Autowired
    private OrdersMapper ordersMapper;        // ← 这个

    @Autowired
    private OrderItemMapper orderItemMapper;  // ← 和这个
    @Autowired
    private RedisLockUtil redisLockUtil;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;        // ← 发延迟消息用


    @Override


    public Map<String, Object> createOrder(CreateOrderDTO dto) {
        Long userId = UserContext.getUserId();
        String lockKey = "lock:order:user:" + userId;
        String owner = redisLockUtil.tryLock(lockKey, 10);
        if (owner == null) {                                 // ③ 没抢到
            throw new BusinessException(429, "操作太频繁，请稍后再试");
        }
        try {
            // ⭐ 改造点 1：把事务结果接住（不再直接 return）
            //    注意：变量名不能叫 result，因为 lambda 内部也有一个叫 result 的局部变量会冲突
            Map<String, Object> orderResult = transactionTemplate.execute(status -> {
                // ─── ① 参数基础校验 ─────────────────────────────────
                if (dto.getCartItemIds() == null || dto.getCartItemIds().isEmpty()) {
                    throw new BusinessException(400, "请选择要购买的商品");
                }
                if (dto.getAddressId() == null) {
                    throw new BusinessException(400, "请选择收货地址");
                }

                // ─── ② 校验地址是不是自己的 ─────────────────────────
                Address address = addressService.getById(dto.getAddressId());
                if (address == null || !address.getUserId().equals(userId)) {
                    throw new BusinessException(403, "收货地址无效");
                }

                // ─── ③ 查这些购物车项 + 校验都是自己的 ──────────────拿dto里面的id查购物车
                List<CartItem> cartItems = cartItemService.listByIds(dto.getCartItemIds());
                if (cartItems.size() != dto.getCartItemIds().size()) {
                    throw new BusinessException(400, "购物车项不存在");
                }
                for (CartItem ci : cartItems) {
                    if (!ci.getUserId().equals(userId)) {
                        throw new BusinessException(403, "无权操作他人购物车");
                    }
                }

                // ─── ④ 批量查商品 + Map 化（复用 Phase B 学过的套路）─拿前端转的查数据库的产品
                List<Long> productIds = new ArrayList<>();
                for (CartItem ci : cartItems) productIds.add(ci.getProductId());//把购物车里面的productid给这个products

                List<Product> products = productService.listByIds(productIds);//拿这个productid去查product
                Map<Long, Product> productMap = new HashMap<>();//定义一个map去装id，和刚查的产品
                for (Product p : products) productMap.put(p.getId(), p);

                // ─── ⑤ 创建订单主表（先算总价）──────────────────────
                BigDecimal totalAmount = BigDecimal.ZERO;
                List<OrderItem> orderItems = new ArrayList<>();      // 待保存的明细，创建订单明细对象的集合

                for (CartItem ci : cartItems) {
                    Product p = productMap.get(ci.getProductId());//通过产品id取出productmap里面的product信息

                    // 校验商品状态
                    if (p == null) throw new BusinessException(400, "商品不存在");
                    if (p.getStatus() == 0) throw new BusinessException(400, "商品已下架：" + p.getName());

                    // 小计 = 单价 × 数量
                    BigDecimal subtotal = p.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
                    totalAmount = totalAmount.add(subtotal);

                    // 构造订单明细（快照！）
                    OrderItem oi = new OrderItem();
                    oi.setProductId(p.getId());
                    oi.setProductName(p.getName());           // ← 快照
                    oi.setProductImage(p.getCoverImage());    // ← 快照
                    oi.setPrice(p.getPrice());                // ← 快照（关键）
                    oi.setQuantity(ci.getQuantity());
                    oi.setSubtotal(subtotal);
                    orderItems.add(oi);
                }

                // 生成订单号：年月日时分秒 + 用户ID + 随机4位
                String orderNo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                        + userId + String.format("%04d", new Random().nextInt(10000));

                Orders order = new Orders();
                order.setOrderNo(orderNo);
                order.setUserId(userId);
                order.setTotalAmount(totalAmount);
                order.setStatus((byte) 0);                    // 0=待付款
                // 地址快照（拼成一个字符串）
                order.setReceiver(address.getReceiver());
                order.setPhone(address.getPhone());
                order.setAddress(address.getProvince() + address.getCity()
                        + address.getDistrict() + address.getDetail());
                order.setRemark(dto.getRemark());

                this.save(order);    // 保存订单主表，自动回填 id

                // ─── ⑥ 给每条明细设置 orderId，批量保存 ─────────────前面创建了orderitem实体类对象，然后加进这个list集合里面了，在这里取出来加进去
                for (OrderItem oi : orderItems) {
                    oi.setOrderId(order.getId());
                }
                orderItemService.saveBatch(orderItems);

                // ─── ⑦ 扣库存（防超卖核心！）────────────────────────
                for (CartItem ci : cartItems) {
                    int rows = productMapper.deductStock(ci.getProductId(), ci.getQuantity());
                    if (rows == 0) {
                        // 0 行被更新 → stock < quantity → 库存不足
                        Product p = productMap.get(ci.getProductId());
                        throw new BusinessException(400, "库存不足：" + p.getName());
                        // 抛异常 → @Transactional 自动回滚之前 save 的订单/明细
                    }
                }

                // ─── ⑧ 清掉这几条购物车 ────────────────────────────
                cartItemService.removeByIds(dto.getCartItemIds());

                // ─── ⑨ 返回 ─────────────────────────────────────
                Map<String, Object> result = new HashMap<>();
                result.put("orderNo", orderNo);
                result.put("orderId", order.getId());
                return result;
            });

            // ⭐ 改造点 2：事务已提交（execute 已返回），现在发延迟消息
            //    为什么放这里？放事务里万一回滚，MQ 收到一条"幽灵订单"消息找不到对应订单
            Long orderId = (Long) orderResult.get("orderId");
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DELAY_EXCHANGE,        // 发到哪个交换机
                    RabbitMQConfig.DELAY_ROUTING_KEY,     // 用什么路由键
                    orderId                                // 消息体：订单 ID
            );

            // ⭐ 改造点 3：最后再 return 给方法外
            return orderResult;
        } finally {
            redisLockUtil.unlock(lockKey, owner);            // ⑤ 必须释放！
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public List<OrderListVO> listMyOrders() {
        Long userId = UserContext.getUserId();

        // ─── ① 查我的所有订单 ──────────────────────────────v查询当前用户的所有订单放进orders
        QueryWrapper<Orders> ow = new QueryWrapper<>();
        ow.eq("user_id", userId).orderByDesc("create_time");
        List<Orders> orders = this.list(ow);

        if (orders.isEmpty()) {
            return new ArrayList<>();
        }

        // ─── ② 收集 orderIds，批量查所有明细 ─────────────────
        List<Long> orderIds = new ArrayList<>();
        for (Orders o : orders) {
            orderIds.add(o.getId());
        }
//根据ordersid查数据库item放进allitems
        QueryWrapper<OrderItem> iw = new QueryWrapper<>();
        iw.in("order_id", orderIds);                 // SQL: WHERE order_id IN (1,2,3)
        List<OrderItem> allItems = orderItemService.list(iw);

        // ─── ③ ⭐ 关键新点：按 orderId 分组成 Map<orderId, List<明细>>
        Map<Long, List<OrderItem>> itemMap = new HashMap<>();
        for (OrderItem item : allItems) {
            // computeIfAbsent: 如果 key 不存在就创建空 list 放进去，然后返回这个 list
            // 等价于下面 3 行：
            //   if (!itemMap.containsKey(item.getOrderId())) {
            //       itemMap.put(item.getOrderId(), new ArrayList<>());
            //   }
            //   itemMap.get(item.getOrderId()).add(item);
            itemMap.computeIfAbsent(item.getOrderId(), k -> new ArrayList<>()).add(item);
        }

        // ─── ④ 循环订单，组装 VO ─────────────────────────────
        List<OrderListVO> result = new ArrayList<>();
        for (Orders o : orders) {
            OrderListVO vo = new OrderListVO();
            vo.setOrderId(o.getId());
            vo.setOrderNo(o.getOrderNo());
            vo.setStatus(o.getStatus());
            vo.setStatusDesc(statusDesc(o.getStatus()));     // 翻译成中文
            vo.setTotalAmount(o.getTotalAmount());
            vo.setReceiver(o.getReceiver());
            vo.setAddress(o.getAddress());
            vo.setCreateTime(o.getCreateTime());

            // 先组装好这单OrderItemVO的明细列表 → 转成 VO 列表
            List<OrderItem> myItems = itemMap.getOrDefault(o.getId(), new ArrayList<>());//从 Map 里取出当前这个订单的所有商品，getOrDefault(...)：
            //通过订单 ID，拿到属于这个订单的所有商品，o.getId()：当前循环到的这个订单的 ID
            //比如订单号 1001
            List<OrderItemVO> itemVOs = new ArrayList<>();//创建一个空集合，准备装 “前端要的商品 VO”
            for (OrderItem item : myItems) {
                OrderItemVO ivo = new OrderItemVO();//创建一个 “前端展示用的商品 VO”
                ivo.setOrderItemId(item.getId());
                ivo.setProductId(item.getProductId());
                ivo.setProductName(item.getProductName());
                ivo.setProductImage(item.getProductImage());
                ivo.setPrice(item.getPrice());
                ivo.setQuantity(item.getQuantity());
                ivo.setSubtotal(item.getSubtotal());
                itemVOs.add(ivo);//把组装好的 VO，放进itemVOs列表里
            }
            vo.setItems(itemVOs);//补充vo最后一个list字段
            result.add(vo);
        }
        return result;
    }

    /**
     * 状态码翻译成中文（私有辅助方法）
     */
    private String statusDesc(Byte status) {
        if (status == null) return "未知";
        switch (status) {
            case 0:
                return "待付款";
            case 1:
                return "已付款";
            case 2:
                return "已发货";
            case 3:
                return "已完成";
            case 4:
                return "已取消";
            default:
                return "未知";
        }
    }

    @Override
    public OrderDetailVO getOrderDetail(Long orderId) {
        Orders orders = ordersMapper.selectById(orderId);
        if (orders == null)
            throw new BusinessException("订单不存在");
        Long userId = UserContext.getUserId();
        if (!orders.getUserId().equals(userId))
            throw new BusinessException("无权限访问");
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId)
        );
        List<OrderItemVO> itemVOs = new ArrayList<>();
        for (OrderItem item : items) {
            OrderItemVO ivo = new OrderItemVO();
            ivo.setOrderItemId(item.getId());
            ivo.setProductId(item.getProductId());
            ivo.setProductName(item.getProductName());
            ivo.setProductImage(item.getProductImage());
            ivo.setPrice(item.getPrice());
            ivo.setQuantity(item.getQuantity());
            ivo.setSubtotal(item.getSubtotal());
            itemVOs.add(ivo);
        }
        OrderDetailVO orderDetailVO = new OrderDetailVO();
        BeanUtils.copyProperties(orders, orderDetailVO);
        orderDetailVO.setOrderId(orders.getId());
        orderDetailVO.setStatusDesc(statusDesc(orders.getStatus()));
        orderDetailVO.setItems(itemVOs);
        return orderDetailVO;
    }

    @Override
    public void cancelOrder(Long orderId) {
        // ─── ① 订单级锁（防双击取消重复还库存）─────
        String lockKey = "lock:order:cancel:" + orderId;
        String owner = redisLockUtil.tryLock(lockKey, 10);
        if (owner == null) {
            throw new BusinessException(429, "操作太频繁，请稍后再试");
        }

        try {
            // ─── ② 事务（必须放在锁内部）
            transactionTemplate.execute(status -> {
                // 1. 查订单 + 判空
                Orders orders = ordersMapper.selectById(orderId);
                if (orders == null)
                    throw new BusinessException("订单为空");
                // 2. 越权防护
                Long userId = UserContext.getUserId();
                if (!orders.getUserId().equals(userId)) {
                    throw new BusinessException("无权限访问");
                }
                // 3. 状态机：status != 0 抛"不可取消"（幂等保障）
                if (orders.getStatus() != 0) {
                    throw new BusinessException("当前订单状态不可取消");
                }
                // 4. 改订单状态为 4（已取消）
                orders.setStatus(OrderStatus.CANCELLED);
                ordersMapper.updateById(orders);
                // 5. 查这个订单的所有明细
                List<OrderItem> items = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>()
                                .eq(OrderItem::getOrderId, orderId)
                );
                // 6. for 每个明细，库存 + 销量回滚
                for (OrderItem item : items) {
                    productMapper.restoreStock(item.getProductId(), item.getQuantity());
                }
                return null;
            });
        } finally {
            // ─── ③ 必须释放锁
            redisLockUtil.unlock(lockKey, owner);
        }
    }

    @Override
    public void payOrder(Long orderId) {
        // ─── ① 订单级锁（防双击支付 / 防同一订单并发请求）─────
        //     注意 key 用 orderId 而不是 userId，
        //     这样用户能同时支付多个订单，但同一订单只能一次
        String lockKey = "lock:order:pay:" + orderId;
        String owner = redisLockUtil.tryLock(lockKey, 10);
        if (owner == null) {
            throw new BusinessException(429, "操作太频繁，请稍后再试");
        }

        try {
            // ─── ② 进入事务（必须放在锁内部，保证锁释放前事务已提交）
            transactionTemplate.execute(status -> {
                // 1. 判空
                Orders orders = ordersMapper.selectById(orderId);
                if (orders == null)
                    throw new BusinessException("订单为空");
                // 2. 越权
                long id = UserContext.getUserId();
                if (!orders.getUserId().equals(id)) {
                    throw new BusinessException("无权访问");
                }
                // 3. 状态机：只有 0 能支付（幂等保障：第二个抢到锁的请求会被这里挡掉）
                if (orders.getStatus() != 0) {
                    throw new BusinessException("不可以进行支付");
                }
                // 4. 改状态 1 + 设 payTime
                orders.setStatus(OrderStatus.PAID);
                orders.setPayTime(LocalDateTime.now());
                ordersMapper.updateById(orders);
                return null;
            });
        } finally {
            // ─── ③ 必须释放锁！否则锁要等 10 秒 TTL 才过期
            redisLockUtil.unlock(lockKey, owner);
        }
    }

    /**
     * MQ 消费者调用：根据订单 ID 关单
     * 跟 cancelOrder 的差异：
     *   1. 不读 UserContext（消费者线程没有登录用户）
     *   2. 状态不是 0 时直接跳过（幂等性：消息可能重复投递）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeOrderByMQ(Long orderId) {
        // 1. 查订单
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            return;                       // 订单都没了，跳过
        }
        // 2. 幂等：只有 0=待付款才关，已支付/已取消/已发货等一律跳过
        if (!order.getStatus().equals(OrderStatus.UNPAID)) {
            return;
        }
        // 3. 改状态为已取消
        order.setStatus(OrderStatus.CANCELLED);
        ordersMapper.updateById(order);
        // 4. 还库存（套用 cancelOrder 的代码）
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );
        for (OrderItem item : items) {
            productMapper.restoreStock(item.getProductId(), item.getQuantity());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int closeTimeoutOrders() {
        // 1. 算出"15 分钟前"的时间点
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(15);

        // 2. 查所有 status=0 且 create_time < threshold 的订单
        List<Orders> timeoutOrders = ordersMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getStatus, OrderStatus.UNPAID)
                        .lt(Orders::getCreateTime, threshold)
        );

        // 3. for 循环每个订单：改状态 + 还库存
        for (Orders o : timeoutOrders) {
            o.setStatus(((OrderStatus.CANCELLED)));
            ordersMapper.updateById(o);
            // 查明细 → 还库存（套用 cancelOrder 的代码）
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, o.getId())
            );
            for (OrderItem item : items) {
                productMapper.restoreStock(item.getProductId(), item.getQuantity());
            }
        }

        return timeoutOrders.size();
    }

}