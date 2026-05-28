package com.minimall.minimall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.minimall.minimall.common.exception.BusinessException;
import com.minimall.minimall.common.util.UserContext;
import com.minimall.minimall.config.RabbitMQConfig;
import com.minimall.minimall.dto.SeckillActivityDTO;
import com.minimall.minimall.entity.Product;
import com.minimall.minimall.entity.SeckillActivity;
import com.minimall.minimall.entity.SeckillOrder;
import com.minimall.minimall.mapper.SeckillActivityMapper;
import com.minimall.minimall.mapper.SeckillOrderMapper;
import com.minimall.minimall.service.IProductService;
import com.minimall.minimall.service.ISeckillActivityService;
import com.minimall.minimall.vo.SeckillActivityVO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 秒杀活动 Service 实现类
 * 注意泛型顺序：ServiceImpl<Mapper, Entity>
 */
@Service
public class SeckillActivityServiceImpl
        extends ServiceImpl<SeckillActivityMapper, SeckillActivity>
        implements ISeckillActivityService {

    @Autowired
    private IProductService productService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;       // ⚠️ String 版（Lua 用）

    @Autowired
    private DefaultRedisScript<Long> seckillStockScript;   // 注入 Lua 脚本 Bean

    @Autowired
    private RabbitTemplate rabbitTemplate;                  // 发 MQ 消息

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;          // 查秒杀订单（Phase 6 用）

    /**
     * 管理员发布秒杀活动
     * 业务校验 → 入库 → 返回新活动 id
     */
    @Override
    public Long publishActivity(SeckillActivityDTO dto) {
        // ─── ① 参数非空校验 ──────────────────────────────
        // TODO 你来写：检查 productId / seckillPrice / stock / startTime / endTime 任何一个为 null 就抛 400
        //     提示：throw new BusinessException(400, "...");
       if(dto.getProductId() == null || dto.getProductId() <= 0){
           throw new BusinessException("产品为空");

       }
        if( dto.getSeckillPrice() ==null|| dto.getSeckillPrice().compareTo(BigDecimal.ZERO) <= 0){
            throw new BusinessException("秒杀价格不存在");
        }
        if(dto.getStock() == null || dto.getStock() <= 0){
            throw new BusinessException("库存不足");

        }
        if(dto.getStartTime() == null){
            throw new BusinessException("开始时间为空");
        }
        if(dto.getEndTime() == null){
            throw new BusinessException("结束时间为空");
        }
        // ─── ② 商品是否存在 + 是否上架 ───────────────────
        // TODO 你来写：
           Product product = productService.getById(dto.getProductId());
           if (product == null) throw new BusinessException("产品为空");
           if (product.getStatus() == 0) throw new BusinessException ("商品已下架");

        // ─── ③ 时间合理性校验 ────────────────────────────
        // TODO
        //     1. startTime 不能早于 now：if (dto.getStartTime().isBefore(LocalDateTime.now())) throw ...;
        //     2. endTime 必须晚于 startTime：if (dto.getEndTime().isBefore(dto.getStartTime())) throw ...;
        if (dto.getStartTime().isBefore(LocalDateTime.now())) throw new BusinessException("还没到开始时间");
        if (dto.getEndTime().isBefore(dto.getStartTime())) throw  new BusinessException("活动时间结束");;
        // ─── ④ 库存和价格校验 ────────────────────────────
        // TODO 你来写：
        //     stock <= 0 抛错
        //     seckillPrice.compareTo(BigDecimal.ZERO) <= 0 抛错（BigDecimal 不能用 <= 直接比）
       Integer stock =product.getStock();
       if(stock<dto.getStock()){
           throw new BusinessException("库存不足");

       }
        if (dto.getSeckillPrice().compareTo(product.getPrice()) >= 0) {
            throw new BusinessException("秒杀价必须低于商品原价");
        }


        // ─── ⑤ 组装实体 ────────────────────────────────

           SeckillActivity activity = new SeckillActivity();
            activity.setProductId(dto.getProductId());
            activity.setSeckillPrice(dto.getSeckillPrice());
           activity.setStock(dto.getStock());
           activity.setStartTime(dto.getStartTime());
            activity.setEndTime(dto.getEndTime());
          activity.setStatus((byte) 0);   // 0=待开始

        // ─── ⑥ 入库（用继承自 IService 的 save 方法）─────
        // TODO 你来写：
           this.save(activity);

        // ─── ⑦ 返回新活动 id ──────────────────────────
        // TODO 你来写：
        //     return activity.getId();   // save 后 MP 自动回填 id

        return activity.getId();   // 把这行删掉，改成 return activity.getId();
    }

    /**
     * 查询"进行中或即将开始"的秒杀活动列表
     * 实现思路：
     *   1. 用 QueryWrapper 查 seckill_activity 表，过滤掉 status=2（已结束）
     *   2. 按 start_time 升序排（即将开始的在前）
     *   3. 收集 productIds，批量查 product 表（避免 N+1 查询）
     *   4. for 循环组装 VO
     */
    @Override
    public List<com.minimall.minimall.vo.SeckillActivityVO> listActiveActivities() {

        // ─── ① 查活动表 ─────────────────────────────────
        // TODO 你来写：
        QueryWrapper<SeckillActivity> w = new QueryWrapper<>();
           w.ne("status", 2)               // status 不等于 2（不是已结束）
            .orderByAsc("start_time");
           List<SeckillActivity> activities = this.list(w);
           if (activities.isEmpty()) return new ArrayList<>();


        // ─── ② 收集所有 productId，批量查商品 ─────────────
        // TODO 你来写：
           List<Long> productIds = new ArrayList<>();
           for (SeckillActivity a : activities) productIds.add(a.getProductId());
          List<Product> products = productService.listByIds(productIds);
        //
           Map<Long, Product> productMap = new HashMap<>();
           for (Product p : products) productMap.put(p.getId(), p);


        // ─── ③ 循环活动，组装 VO ─────────────────────────
        // TODO 你来写：
           List<SeckillActivityVO> result = new ArrayList<>();
           for (SeckillActivity a : activities) {
              Product p = productMap.get(a.getProductId());
              SeckillActivityVO vo = new SeckillActivityVO();
            vo.setId(a.getId());
              vo.setProductId(a.getProductId());
              vo.setProductName(p != null ? p.getName() : null);
              vo.setProductImage(p != null ? p.getCoverImage() : null);
              vo.setOriginalPrice(p != null ? p.getPrice() : null);
              vo.setSeckillPrice(a.getSeckillPrice());
              vo.setStock(a.getStock());
               vo.setStartTime(a.getStartTime());
              vo.setEndTime(a.getEndTime());
              vo.setStatus(a.getStatus());
               vo.setStatusDesc(statusDesc(a.getStatus()));
               result.add(vo);
           }
           return result;

    }

    /**
     * 秒杀核心入口
     *
     * 流程：
     *   1. 查活动 → 校验存在 + 时间窗口
     *   2. 库存懒加载到 Redis（如果还没预热）
     *   3. 调 Lua 原子扣库存
     *   4. 根据返回值：抢到 → 发 MQ；其他 → 抛异常
     */
    @Override
    public String seckill(Long activityId) {
        Long userId = UserContext.getUserId();

        // ─── ① 校验活动 ─────────────────────────────────
        // TODO 你来写：
          SeckillActivity activity = this.getById(activityId);
          if (activity == null) throw new BusinessException("活动不存在");
          LocalDateTime now = LocalDateTime.now();
          if (now.isBefore(activity.getStartTime())) throw new BusinessException("活动还未开始");
           if (now.isAfter(activity.getEndTime()))   throw new BusinessException("活动已结束");


        // ─── ② Redis 库存懒加载 ─────────────────────────
        // TODO 你来写（暂时简化版，不防并发预热）：
          String stockKey  = "seckill:stock:"  + activityId;
          String boughtKey = "seckill:bought:" + activityId;
          if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(stockKey))) {
               stringRedisTemplate.opsForValue().set(stockKey, activity.getStock().toString());
          }


        // ─── ③ ⭐ 调 Lua 原子扣库存 ─────────────────────
        // TODO 你来写：
          Long result = stringRedisTemplate.execute(
              seckillStockScript,
              java.util.Arrays.asList(stockKey, boughtKey),
               userId.toString()
           );


        // ─── ④ 根据 Lua 返回值分支 ───────────────────────
        // TODO 你来写（4 个分支）：
          if (result == null)        throw new BusinessException("系统错误");
           if (result == -2L)         throw new BusinessException("活动不存在或未开始");
          if (result == -1L)         throw new BusinessException("您已参与过此次秒杀");
          if (result == 0L)          throw new BusinessException("已售罄");
           if (result == 1L) {// 抢到了！发 MQ 异步生成订
                String msg = activityId + ":" + userId;
              rabbitTemplate.convertAndSend(
                  RabbitMQConfig.SECKILL_EXCHANGE,
                   RabbitMQConfig.SECKILL_ROUTING_KEY,
                   msg
              );
              return "抢购成功，订单生成中，请稍后查询";
          }
          throw new BusinessException("未知错误");

    }

    /**
     * 查询"我的秒杀结果"（前端轮询用）
     *
     * 判断逻辑：
     *   1. 查 DB seckill_order：有 → SUCCESS + orderNo
     *   2. 没有 → 查 Redis SISMEMBER bought：在 → PROCESSING
     *   3. 也不在 → NOT_FOUND
     */
    @Override
    public Map<String, Object> querySeckillResult(Long activityId) {
        Long userId = UserContext.getUserId();
        Map<String, Object> result = new HashMap<>();

        // ─── ① 查 DB：是否已生成订单 ───────────────────
        // TODO 你来写：
           QueryWrapper<SeckillOrder> w = new QueryWrapper<>();
           w.eq("user_id", userId).eq("seckill_activity_id", activityId);
           SeckillOrder order = seckillOrderMapper.selectOne(w);
          if (order != null) {
               result.put("status", "SUCCESS");
              result.put("orderNo", order.getOrderNo());
               result.put("message", "下单成功，请尽快支付");
              return result;
           }


        // ─── ② DB 没有 → 查 Redis：是否在已购集合 ───────
        // TODO 你来写：
          String boughtKey = "seckill:bought:" + activityId;
          Boolean isMember = stringRedisTemplate.opsForSet().isMember(boughtKey, userId.toString());
           if (Boolean.TRUE.equals(isMember)) {
              result.put("status", "PROCESSING");
              result.put("orderNo", null);
              result.put("message", "订单生成中，请稍后再查");
              return result;
           }


        // ─── ③ 都没有 → 没抢到 ─────────────────────────
           result.put("status", "NOT_FOUND");
           result.put("orderNo", null);
           result.put("message", "未抢到，请下次再来");
           return result;
    }

    /** 状态码翻译成中文 */
    private String statusDesc(Byte status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待开始";
            case 1: return "进行中";
            case 2: return "已结束";
            default: return "未知";
        }
    }
}
