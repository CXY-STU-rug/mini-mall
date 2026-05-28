package com.minimall.minimall.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.minimall.minimall.dto.SeckillActivityDTO;
import com.minimall.minimall.entity.SeckillActivity;
import com.minimall.minimall.vo.SeckillActivityVO;

import java.util.List;
import java.util.Map;

/**
 * 秒杀活动 Service 接口
 * 继承 IService<SeckillActivity> 白嫖 save/list/getById/page 等通用方法
 */
public interface ISeckillActivityService extends IService<SeckillActivity> {

    /**
     * 管理员发布秒杀活动
     * @return 新活动的 id
     */
    Long publishActivity(SeckillActivityDTO dto);

    /**
     * 查询"进行中或即将开始"的秒杀活动列表（按开始时间升序）
     * 已结束（status=2）的活动不返回
     */
    List<SeckillActivityVO> listActiveActivities();

    /**
     * 秒杀核心入口
     * 调 Lua 原子扣库存 → 抢到则发 MQ 异步下单
     * @return 提示语（"排队中..."）
     */
    String seckill(Long activityId);

    /**
     * 查询"我的秒杀结果"（前端轮询用）
     * @return Map: {status, orderNo, message}
     *   status 取值：SUCCESS / PROCESSING / NOT_FOUND
     */
    Map<String, Object> querySeckillResult(Long activityId);
}
