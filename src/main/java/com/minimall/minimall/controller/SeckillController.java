package com.minimall.minimall.controller;

import com.minimall.minimall.common.result.Result;
import com.minimall.minimall.dto.SeckillActivityDTO;
import com.minimall.minimall.service.ISeckillActivityService;
import com.minimall.minimall.vo.SeckillActivityVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 秒杀活动 / 秒杀下单 接口
 * 后续 Phase 3-6 会继续往这个 Controller 里加方法
 */
@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    @Autowired
    private ISeckillActivityService seckillActivityService;

    /**
     * 管理员发布秒杀活动
     * 暂不做角色校验（后续 RBAC 阶段补）
     */
    @PostMapping("/activity")
    public Result<Long> publishActivity(@RequestBody SeckillActivityDTO dto) {
        Long id = seckillActivityService.publishActivity(dto);
        return Result.success(id);
    }

    /**
     * 查询进行中/即将开始的秒杀活动列表
     * 任何登录用户都能调
     */
    @GetMapping("/activities")
    public Result<List<SeckillActivityVO>> listActivities() {
        return Result.success(seckillActivityService.listActiveActivities());
    }

    /**
     * 秒杀核心接口
     * 真正的高并发入口，背后是 Lua 原子扣库存 + MQ 异步下单
     */
    @PostMapping("/{activityId}")
    public Result<String> seckill(@PathVariable Long activityId) {
        return Result.success(seckillActivityService.seckill(activityId));
    }

    /**
     * 查询"我的秒杀结果"（前端轮询用）
     * 返回 {status, orderNo, message}
     *   - SUCCESS    : 订单已生成
     *   - PROCESSING : 抢到了但订单还在生成中
     *   - NOT_FOUND  : 没抢到
     */
    @GetMapping("/result/{activityId}")
    public Result<Map<String, Object>> querySeckillResult(@PathVariable Long activityId) {
        return Result.success(seckillActivityService.querySeckillResult(activityId));
    }
}
