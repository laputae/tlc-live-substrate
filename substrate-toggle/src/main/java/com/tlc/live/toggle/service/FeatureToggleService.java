package com.tlc.live.toggle.service;

import com.tlc.live.toggle.config.ToggleProperties;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存热开关中枢。
 *
 * <p>直播人数从 10 万暴涨到 100 万（明星空降）时，需要一键切断 AI 情绪分析、
 * 花哨弹幕等非核心链路，全力保红包与带货。读取方（网关/AI 中枢）通过 REST
 * 轮询本服务（推拉模型中的"拉"），进程内以 ConcurrentHashMap 保存热状态，
 * 读取路径无锁、无网络。接入 Nacos 后仅需替换配置来源，接口不变。
 */
@Service
public class FeatureToggleService {

    /** 一键保命开关：开启后所有非核心链路立即降级。 */
    public static final String KILL_SWITCH = "core-only";

    private final ToggleProperties properties;
    private final Map<String, Boolean> toggles = new ConcurrentHashMap<>();

    public FeatureToggleService(ToggleProperties properties) {
        this.properties = properties;
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
    }

    /** 当前全量快照（供网关定时拉取刷新本地 volatile 缓存）。 */
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
    }
}
