package com.tlc.live.common.event;

/**
 * 抢红包（秒杀）事件：走风控短路查杀 + RocketMQ 削峰 + Redis Lua 原子扣减。
 *
 * @param userId      抢包用户
 * @param roomId      直播间
 * @param redPacketId 红包雨批次 ID
 */
public record RushRedPacketEvent(long userId, String roomId, long redPacketId) implements LiveEvent {
}
