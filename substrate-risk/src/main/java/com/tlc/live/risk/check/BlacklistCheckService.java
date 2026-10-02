package com.tlc.live.risk.check;

import com.tlc.live.risk.config.RiskProperties;
import com.tlc.live.common.exception.RiskRejectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 校验一：设备指纹黑名单（模拟外部黑产库 I/O）。
 *
 * <p>外部源不可用时选择 fail-open（放行并告警）：秒杀是低资损场景，
 * 可用性优先于风控完备性；资损敏感场景应改为 fail-close。
 */
@Service
public class BlacklistCheckService {

    private static final Logger log = LoggerFactory.getLogger(BlacklistCheckService.class);

    public static final String BLACKLIST_KEY = "risk:blacklist";

    private final StringRedisTemplate redis;
    private final RiskProperties props;

    public BlacklistCheckService(StringRedisTemplate redis, RiskProperties props) {
        this.redis = redis;
        this.props = props;
    }

    /** 命中黑名单直接抛出 RiskRejectException：在结构化并发作用域内将立即取消其余校验的 I/O 等待。 */
    public void check(long userId) {
        simulateRemoteIo();
        Boolean hit;
        try {
            hit = redis.opsForSet().isMember(BLACKLIST_KEY, String.valueOf(userId));
        } catch (Exception ex) {
            log.warn("黑名单源不可用，降级放行 userId={}: {}", userId, ex.getMessage());
            return;
        }
        if (Boolean.TRUE.equals(hit)) {
            throw new RiskRejectException(userId, "设备指纹命中黑名单");
        }
    }

    private void simulateRemoteIo() {
        if (props.getSimulateLatencyMs() > 0) {
            try {
                Thread.sleep(props.getSimulateLatencyMs());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RiskRejectException(-1, "风控 I/O 被中断");
            }
        }
    }
}
