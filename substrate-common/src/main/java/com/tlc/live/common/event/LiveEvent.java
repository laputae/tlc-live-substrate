package com.tlc.live.common.event;

/**
 * 全局上行事件契约（密封接口）。
 *
 * <p>系统只允许三类核心事件存在，配合 {@code switch pattern matching}
 * 即可在网关层做无状态的数据导向路由（DOP），无需 instanceof 链与访问者模式。
 */
public sealed interface LiveEvent permits DanmakuEvent, RushRedPacketEvent, SystemBanEvent {

    /** 事件关联的直播间（SystemBanEvent 可为 null）。 */
    String roomId();
}
