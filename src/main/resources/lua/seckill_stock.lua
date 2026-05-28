-- ============================================
-- 秒杀预扣库存脚本
-- KEYS[1] = seckill:stock:{activityId}    库存 key
-- KEYS[2] = seckill:bought:{activityId}   已购用户 Set key
-- ARGV[1] = userId                         用户 ID
--
-- 返回值:
--   1  = 抢到了
--   0  = 库存不足
--  -1  = 该用户已抢过
--  -2  = 活动 key 不存在（未预热）
-- ============================================

-- ① 检查活动是否预热
if redis.call('EXISTS', KEYS[1]) == 0 then
    return -2
end

-- ② 检查用户是否已抢过
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return -1
end

-- ③ 检查库存（必须 tonumber）
local stock = tonumber(redis.call('GET', KEYS[1]))
if stock <= 0 then
    return 0
end

-- ④ 原子扣库存 + 记录已购
redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])

return 1
