-- 红包雨原子扣减（单脚本完成：幂等 + 凭证弹出 + 中奖记录 + 已参与标记 + 持久化入队）
-- Java 侧仅在抢中后做一次 PUBLISH 广播，扣减路径总共 2 次 Redis 往返
--
-- KEYS[1] 库存凭证队列 rp:stock:{id}     (value: "redPacketId:seq:amountFen")
-- KEYS[2] 中奖名单哈希   rp:winners:{id}  (field=userId, value=amountFen)
-- KEYS[3] 幂等去重集合   rp:dedup:{id}
-- KEYS[4] 已参与标记     rp:rushed:{id}   (供风控预过滤)
-- KEYS[5] 持久化队列     rp:persist:queue
-- ARGV[1] userId
-- ARGV[2] roomId
-- ARGV[3] redPacketId
--
-- 返回: >0 抢中（金额，分）  1 已抢过  2 已抢完  -1 异常

local stockKey = KEYS[1]
local winnersKey = KEYS[2]
local dedupKey = KEYS[3]
local rushedKey = KEYS[4]
local persistQueue = KEYS[5]
local userId = ARGV[1]
local roomId = ARGV[2]
local redPacketId = ARGV[3]

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

-- 3. 中奖记录 + 去重 + 已参与标记 + 持久化入队（全部在本脚本内原子完成）
redis.call('SADD', dedupKey, userId)
redis.call('HSET', winnersKey, userId, amountFen)
redis.call('SET', 'rp:ticket:' .. ticketId .. ':' .. seq, userId)
redis.call('SADD', rushedKey, userId)
redis.call('LPUSH', persistQueue,
    '{"redPacketId":' .. redPacketId .. ',"roomId":"' .. roomId .. '","userId":' .. userId .. ',"amountFen":' .. amountFen .. '}')

return tonumber(amountFen)