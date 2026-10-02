package com.tlc.live.risk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 风控窗口配置。
 */
@ConfigurationProperties(prefix = "tlc.risk")
public class RiskProperties {

    /** 端到端风控判定窗口：超时即熔断拒绝，绝不让外部 API 延迟拖垮抢红包链路。 */
    private long timeoutMs = 50;

    /** 模拟远程风控源（指纹库/信誉分）的 I/O 延迟，用于本地联调演示并发短路，生产置 0。 */
    private long simulateLatencyMs = 0;

    /** 信誉分低于该值直接拒绝。 */
    private int creditRejectBelow = 60;

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public long getSimulateLatencyMs() {
        return simulateLatencyMs;
    }

    public void setSimulateLatencyMs(long simulateLatencyMs) {
        this.simulateLatencyMs = simulateLatencyMs;
    }

    public int getCreditRejectBelow() {
        return creditRejectBelow;
    }

    public void setCreditRejectBelow(int creditRejectBelow) {
        this.creditRejectBelow = creditRejectBelow;
    }
}
