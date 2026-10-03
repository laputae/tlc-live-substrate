package com.tlc.live.toggle.service;

import com.tlc.live.toggle.config.ToggleProperties;
import com.tlc.live.toggle.event.TogglesChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存热开关中枢。
 *
 * <p>变更后发布 TogglesChangedEvent：Nacos 同步组件监听该事件，
 * 把快照推送（publish）到配置中心；网关等读取方通过 Nacos 长轮询
 * 近实时感知——这就是推拉模型中的"推"。
 */
@Service
public class FeatureToggleService {

    /** 一键保命开关：开启后所有非核心开关立即降级。 */
    public static final String KILL_SWITCH = "core-only";

    private final ToggleProperties properties;
    private final Map<String, Boolean> toggles = new ConcurrentHashMap<>();
    private final ApplicationEventPublisher publisher;

    public FeatureToggleService(ToggleProperties properties, ApplicationEventPublisher publisher) {
        this.properties = properties;
        this.publisher = publisher;
        this.toggles.putAll(properties.getDefaults());
    }

    /** 读取开关当前状态（热路径，无锁）。 */
    public boolean isEnabled(String key) {
        Boolean current = toggles.get(key);
        if (current != null) {
            return current;
        }
        return properties.getDefaults().getOrDefault(key, false);
    }

    public void set(String key, boolean enabled) {
        toggles.put(key, enabled);
        publisher.publishEvent(new TogglesChangedEvent(snapshot()));
    }

    public void applySnapshot(Map<String, Boolean> snapshot) {
        toggles.putAll(snapshot);
    }

    /** 当前全量快照。 */
    public Map<String, Boolean> snapshot() {
        Map<String, Boolean> view = new LinkedHashMap<>(properties.getDefaults());
        view.putAll(toggles);
        return view;
    }

    /**
     * 一键保命：开启时切断全部非核心开关，关闭时恢复默认值。
     * 核心链路（风控、秒杀）在任何情况下都不受影响。
     */
    public void killSwitch(boolean on) {
        if (on) {
            properties.getDefaults().keySet().stream()
                    .filter(key -> !properties.getCoreKeys().contains(key))
                    .forEach(key -> toggles.put(key, false));
        } else {
            properties.getDefaults().forEach(toggles::put);
        }
        toggles.put(KILL_SWITCH, on);
        publisher.publishEvent(new TogglesChangedEvent(snapshot()));
    }
}