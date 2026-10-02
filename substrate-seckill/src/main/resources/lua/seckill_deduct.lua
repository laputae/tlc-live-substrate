-- 红包雨原子扣减（一个脚本内完成：幂等校验 + 凭证弹出 + 中奖记录，天然无并发超卖）
--
-- KEYS[1]: 红包库存凭证队列 rp:stock:{redPacketId}   （value 形如 "ticketId:amountFen"）
-- KEYS[2]: 中奖名单哈希   rp:winners:{redPacketId}   （field=userId, value=amountFen）
-- KEYS[3]: 幂等去重集合   rp:dedup:{redPacketId}
-- ARGV[1]: userId
--
-- 返回: >0 抢中（返回值为本次到账金额，单位分）
--        1 已抢过（幂等拒绝）
--        2 已抢完
--       -1 脚本执行异常

local stockKey = KEYS[1]
local winnersKey = KEYS[2]
local dedupKey = KEYS[3]
local userId = ARGV[1]

-- 1. 幂等校验：一人一包
if redis.call('SISMEMBER', dedupKey, userId) == 1 then
    return 1
end

-- 2. 原子弹出一个凭证（金额在发红包时已按二倍均值法预拆分）
local ticket = redis.call('LPOP', stockKey)
if not ticket then
    return 2
end

local ticketId, seq, amountFen = string.match(ticket, '^(%d+):(%d+):(%d+)$')
if not ticketId or not seq or not amountFen then
    return -1
end

-- 3. 记录中奖名单与去重标记（同脚本内原子完成）
redis.call('SADD', dedupKey, userId)
redis.call('HSET', winnersKey, userId, amountFen)
redis.call('SET', 'rp:ticket:' .. ticketId .. ':' .. seq, userId)

return tonumber(amountFen)
