package com.tlc.live.common.event;

/**
 * 系统封禁/管控事件：由风控服务产生，回灌至网关与下行推送集群。
 *
 * @param targetUserId 被处置用户
 * @param reason       处置原因
 * @param roomId       直播间
 */
public record SystemBanEvent(long targetUserId, String roomId, String reason) implements LiveEvent {
}
