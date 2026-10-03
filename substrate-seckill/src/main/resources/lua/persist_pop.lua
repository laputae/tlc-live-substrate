-- 批量弹出持久化队列（单次往返取一整批，替代逐条 rightPop 的 N 次往返）
-- KEYS[1] rp:persist:queue   ARGV[1] 批量大小
local items = redis.call('LRANGE', KEYS[1], 0, tonumber(ARGV[1]) - 1)
if #items > 0 then
    redis.call('LTRIM', KEYS[1], tonumber(ARGV[1]), -1)
end
return items