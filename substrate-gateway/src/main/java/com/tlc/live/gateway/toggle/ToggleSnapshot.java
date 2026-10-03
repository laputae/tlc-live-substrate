package com.tlc.live.gateway.toggle;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 网关本地开关快照：由 NacosToggleWatcher 在配置变更时刷新（volatile 整体替换）。
 *
 * <p>读路径纯内存、无锁、无网络——即使 Nacos 挂掉，也用最后一次快照继续服务；
 * 快照为空（Nacos 从未可用）时全部放行，保证可用性优先。
 */
@Component
public class ToggleSnapshot {

    public static final String KILL_SWITCH = "core-only";

    private final Set<String> coreKeys = new CopyOnWriteArraySet<>(java.util.Set.of("risk-core", "seckill-core"));
    private volatile Map<String, Boolean> snapshot = Map.of();

    public void update(Map<String, Boolean> snapshot) {
        this.snapshot = snapshot.isEmpty() ? this.snapshot : Map.copyOf(snapshot);
    }

    public Map<String, Boolean> current() {
        return snapshot;
    }

    /** 判定某开关在网关侧是否放行（含一键保命语义）。 */
    public boolean isEnabled(String key) {
        Map<String, Boolean> s = snapshot;
        if (s.isEmpty()) {
            return true;
        }
        if (Boolean.TRUE.equals(s.get(KILL_SWITCH)) && !coreKeys.contains(key)) {
            return false;
        }
        return s.getOrDefault(key, true);
    }
}