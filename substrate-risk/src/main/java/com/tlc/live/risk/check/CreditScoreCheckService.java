package com.tlc.live.risk.check;

import com.tlc.live.common.exception.RiskRejectException;
import com.tlc.live.risk.config.RiskProperties;
import com.tlc.live.risk.precheck.RiskPrecheckService;
import org.springframework.stereotype.Service;

/**
 * 校验二：信誉分查询（生产环境为外部信誉中心 I/O）。
 * 画像由 RiskPrecheckService 本地缓存（信誉分 TTL 60s）。
 */
@Service
public class CreditScoreCheckService {

    private final RiskPrecheckService precheck;
    private final RiskProperties props;

    public CreditScoreCheckService(RiskPrecheckService precheck, RiskProperties props) {
        this.precheck = precheck;
        this.props = props;
    }

    public void check(long userId) {
        simulateRemoteIo();
        int score = precheck.creditScore(userId);
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