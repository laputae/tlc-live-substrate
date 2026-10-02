package com.tlc.live.common.exception;

/**
 * 库存耗尽异常：Redis Lua 原子扣减返回无可用凭证时抛出。
 */
public class SecKillSoldOutException extends BizException {

    public static final String CODE = "SOLD_OUT";

    public SecKillSoldOutException(long redPacketId) {
        super(CODE, "红包批次[%d]已被抢完".formatted(redPacketId));
    }
}
