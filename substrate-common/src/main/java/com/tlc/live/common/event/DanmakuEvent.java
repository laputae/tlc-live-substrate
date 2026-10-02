package com.tlc.live.common.event;

/**
 * 纯互动弹幕事件：走 AI 中枢做情绪聚合，不进入交易链路。
 *
 * <p>record 为不可变数据载体，Project Valhalla 值类特性落地后
 * 可直接升级为 {@code value record}，实现平铺分配、消除 GC 抖动。
 *
 * @param userId  发送者
 * @param roomId  直播间
 * @param content 弹幕内容
 */
public record DanmakuEvent(long userId, String roomId, String content) implements LiveEvent {
}
