package com.tlc.live.toggle.web;

import com.tlc.live.toggle.service.FeatureToggleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 开关读写入口：网关等读取方定时拉取快照刷新本地缓存，运维侧手动改开关。
 */
@RestController
@RequestMapping("/api/toggles")
public class ToggleController {

    private final FeatureToggleService toggleService;

    public ToggleController(FeatureToggleService toggleService) {
        this.toggleService = toggleService;
    }

    /** 全量快照：读取方建议每 3~5 秒拉取一次。 */
    @GetMapping
    public Map<String, Boolean> snapshot() {
        return toggleService.snapshot();
    }

    @GetMapping("/{key}")
    public Map<String, Object> get(@PathVariable String key) {
        return Map.of("key", key, "enabled", toggleService.isEnabled(key));
    }

    @PutMapping("/{key}")
    public Map<String, Object> set(@PathVariable String key, @RequestBody Map<String, Boolean> body) {
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        toggleService.set(key, enabled);
        return Map.of("key", key, "enabled", enabled);
    }

    /** 一键保命：?enabled=true 切断全部非核心链路。 */
    @PostMapping("/kill-switch")
    public Map<String, Object> killSwitch(@RequestParam(defaultValue = "true") boolean enabled) {
        toggleService.killSwitch(enabled);
        return Map.of("killSwitch", enabled, "toggles", toggleService.snapshot());
    }
}
