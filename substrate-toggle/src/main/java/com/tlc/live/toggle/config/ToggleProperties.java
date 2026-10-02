package com.tlc.live.toggle.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * 特征开关默认值与核心链路清单。
 */
@ConfigurationProperties(prefix = "tlc.toggle")
public class ToggleProperties {

    /** 各开关默认值，如 ai-sentiment: true、fancy-danmaku: true。 */
    private Map<String, Boolean> defaults = Map.of();

    /** 核心链路开关：保命模式下必须保留，绝不允许被切断。 */
    private List<String> coreKeys = List.of("risk-core", "seckill-core");

    public Map<String, Boolean> getDefaults() {
        return Map.copyOf(defaults);
    }

    public void setDefaults(Map<String, Boolean> defaults) {
        this.defaults = defaults == null ? Map.of() : Map.copyOf(defaults);
    }

    public List<String> getCoreKeys() {
        return List.copyOf(coreKeys);
    }

    public void setCoreKeys(List<String> coreKeys) {
        this.coreKeys = coreKeys == null ? List.of() : List.copyOf(coreKeys);
    }
}
