package com.tlc.live.seckill.model;

/**
 * AI 中枢下发的红包雨决策事件（RocketMQ topic: tlc-agent-decision）。
 *
 * @param action   动作标识，仅接受 DROP_REDPACKET
 * @param roomId   直播间
 * @param totalFen 总金额（分）
 * @param count    红包个数
 */
public record RedPacketDropEvent(String action, String roomId, int totalFen, int count) {

    public static final String ACTION_DROP = "DROP_REDPACKET";
}
