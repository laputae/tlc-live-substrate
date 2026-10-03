package com.tlc.live.toggle.event;

import java.util.Map;

/**
 * 开关变更事件：本地开关一变即发布，由 Nacos 同步组件推送到配置中心（push 模式）。
 */
public record TogglesChangedEvent(Map<String, Boolean> snapshot) {
}