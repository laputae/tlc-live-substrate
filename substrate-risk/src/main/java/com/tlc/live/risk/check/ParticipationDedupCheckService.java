package com.tlc.live.risk.check;

import com.tlc.live.common.exception.RiskRejectException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 校验三：重复参与校验（同一红包批次一人一包，Redis 集合去重）。
 */
@Service
public class ParticipationDedupCheckService {

    private final StringRedisTemplate redis;

    public ParticipationDedupCheckService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void check(long userId, long redPacketId) {
        Boolean rushed;
        try {
            rushed = redis.opsForSet().isMember("rp:rushed:" + redPacketId, String.valueOf(userId));
        } catch (Exception ex) {
            return; // 与其余校验一致：源不可用放行，最终一致性由秒杀服务 Lua 幂等兜底
        }
        if (Boolean.TRUE.equals(rushed)) {
            throw new RiskRejectException(userId, "重复参与红包批次 " + redPacketId);
        }
    }
}
