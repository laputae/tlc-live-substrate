package com.tlc.live.common.exception;

/**
 * 风控短路异常：黑灰产命中后由结构化并发强制中断链路时抛出。
 */
public class RiskRejectException extends BizException {

    public static final String CODE = "RISK_REJECT";

    public RiskRejectException(long userId, String reason) {
        super(CODE, "用户[%d]被风控拦截: %s".formatted(userId, reason));
    }
}
