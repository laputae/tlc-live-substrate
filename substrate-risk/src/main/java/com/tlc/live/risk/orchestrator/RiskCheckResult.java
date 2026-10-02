package com.tlc.live.risk.orchestrator;

/**
 * 风控判定结果。
 *
 * @param userId    用户
 * @param passed    是否放行
 * @param reason    拒绝原因（放行为 "OK"）
 * @param latencyMs 端到端判定耗时
 */
public record RiskCheckResult(long userId, boolean passed, String reason, long latencyMs) {

    private static final String OK = "OK";

    public static RiskCheckResult pass(long userId, long latencyMs) {
        return new RiskCheckResult(userId, true, OK, latencyMs);
    }

    public static RiskCheckResult reject(long userId, String reason, long latencyMs) {
        return new RiskCheckResult(userId, false, reason, latencyMs);
    }
}
