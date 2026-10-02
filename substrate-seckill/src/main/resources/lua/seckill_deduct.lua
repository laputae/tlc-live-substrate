-- KEYS[1]: 红包库存凭证队列 rp:stock:{redPacketId}
-- KEYS[2]: 中奖名单集合     rp:winners:{redPacketId}
-- KEYS[3]: 幂等去重集合     rp:dedup:{redPacketId}
-- ARGV[1]: userId
-- ARGV[2]: 当前时间戳(ms)
-- 返回: 0=抢中 1=已抢过 2=已抢完

local stockKey = KEYS[1]
local winnersKey = KEYS[2]
local dedupKey = KEYS[3]
local userId = ARGV[1]
local now = ARGV[2]

-- 1. 幂等校验：一人一包
if redis.call('SISMEMBER', dedupKey, userId) == 1 then
    return 1
end

-- 2. 原子弹出一个凭证，无剩余即售罄
local ticket = redis.call('LPOP', stockKey)
if not ticket then
    return 2
end

-- 3. 记录中奖名单与去重标记（单个脚本内原子完成，无并发超卖）
redis.call('SADD', dedupKey, userId)
redis.call('HSET', winnersKey, userId, now)
redis.call('SET', 'rp:ticket:' .. ticket, userId)

return 0
