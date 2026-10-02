package com.tlc.live.seckill.model;

/**
 * 用户抢红包请求（风控放行后进入 MQ：topic tlc-seckill-rush）。
 *
 * @param userId      用户
 * @param roomId      直播间
 * @param redPacketId 红包批次
 */
public record RushRequest(long userId, String roomId, long redPacketId) {
}
