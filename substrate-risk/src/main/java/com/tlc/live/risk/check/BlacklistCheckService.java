package com.tlc.live.risk.check;

import com.tlc.live.common.exception.RiskRejectException;
import com.tlc.live.risk.config.RiskProperties;
import com.tlc.live.risk.precheck.RiskPrecheckService;
import org.springframework.stereotype.Service;

/**
 * 校验一：设备指纹黑名单（生产环境为外部黑产库 I/O）。
 *
 * <p>画像由 {@link RiskPrecheckService} 本地缓存（黑名单 TTL 30s），
 * 命中后 0 次 Redis 访问。命中黑名单抛出 RiskRejectException：
 * 在结构化并发作用域内将立即取消其余校验的 I/O 等待。
 * 新拉黑用户最多延迟 30s 生效（与 fail-open 策略一致的可用性取舍）。
 */
@Service
public class BlacklistCheckService {

    private final RiskPrecheckService precheck;
    private final RiskProperties props;

    public BlacklistCheckService(RiskPrecheckService precheck, RiskProperties props) {
        this.precheck = precheck;
        this.props = props;
    }

    public void check(long userId) {
        simulateRemoteIo();
        if (precheck.isBlacklisted(userId)) {
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