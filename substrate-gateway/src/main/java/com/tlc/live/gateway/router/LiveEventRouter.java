package com.tlc.live.gateway.router;

import com.tlc.live.common.event.DanmakuEvent;
import com.tlc.live.common.event.LiveEvent;
import com.tlc.live.common.event.RushRedPacketEvent;
import com.tlc.live.common.event.SystemBanEvent;
import org.springframework.stereotype.Component;

/**
 * 无状态数据导向路由引擎（DOP）。
 *
 * <p>基于 {@code switch pattern matching} 对密封事件类型做穷尽匹配：
 * 纯互动流量（弹幕）送 AI 中枢，交易流量（秒杀）送风控拦截，管控事件直接下发。
 * 路由过程无多态分发、无反射、无中间对象，可被 JIT 完全内联。
 */
@Component
public class LiveEventRouter {

    public enum Destination {
        AI_AGENT, RISK_CONTROL, DOWNSTREAM_PUSH, DISCARD
    }

    public Destination route(LiveEvent event) {
        return switch (event) {
            case DanmakuEvent e when e.content() == null || e.content().isBlank() -> Destination.DISCARD;
            case DanmakuEvent e -> Destination.AI_AGENT;
            case RushRedPacketEvent e when e.redPacketId() <= 0 -> Destination.DISCARD;
            case RushRedPacketEvent e -> Destination.RISK_CONTROL;
            case SystemBanEvent e -> Destination.DOWNSTREAM_PUSH;
        };
    }
}
