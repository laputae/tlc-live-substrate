package com.tlc.live.risk.check;

import com.tlc.live.common.exception.RiskRejectException;
import com.tlc.live.risk.config.RiskProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 校验二：信誉分查询（模拟外部信誉中心 I/O）。
 */
@Service
public class CreditScoreCheckService {

    public static final String CREDIT_KEY = "risk:credit"; // hash: userId -> score

    private final StringRedisTemplate redis;
    private final RiskProperties props;

    public CreditScoreCheckService(StringRedisTemplate redis, RiskProperties props) {
        this.redis = redis;
        this.props = props;
    }

    public void check(long userId) {
        simulateRemoteIo();
        int score;
        try {
            Object value = redis.opsForHash().get(CREDIT_KEY, String.valueOf(userId));
            score = value == null ? 100 : Integer.parseInt(value.toString());
        } catch (Exception ex) {
            score = 100; // 源不可用按正常用户处理（与黑名单一致的 fail-open 策略）
        }
        if (score < props.getCreditRejectBelow()) {
            throw new RiskRejectException(userId, "信誉分过低: " + score);
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
