-- 风控预检：黑名单 + 信誉分一次 EVALSHA 完成（缓存未命中时才触发）
-- KEYS[1] 黑名单集合 risk:blacklist
-- KEYS[2] 信誉分哈希 risk:credit
-- ARGV[1] userId
-- 返回: {isBlacklisted(0/1), score(不存在时为 100)}
local black = redis.call('SISMEMBER', KEYS[1], ARGV[1])
local score = redis.call('HGET', KEYS[2], ARGV[1])
if not score then
    score = 100
end
return {black, score}