package com.tlc.live.risk.orchestrator;

/**
 * 风控判定请求。
 *
 * @param userId      抢红包用户
 * @param roomId      直播间
 * @param redPacketId 红包批次 ID
 */
public record RiskCheckRequest(long userId, String roomId, long redPacketId) {
}
