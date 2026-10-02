package com.tlc.live.agent.model;

public record RedPacketDecision(String action, String roomId, int totalFen, int count) {

    public static final String ACTION_DROP = "DROP_REDPACKET";

    public static RedPacketDecision drop(String roomId, int totalFen, int count) {
        return new RedPacketDecision(ACTION_DROP, roomId, totalFen, count);
    }
}
