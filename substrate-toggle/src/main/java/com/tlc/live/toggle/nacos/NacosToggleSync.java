package com.tlc.live.toggle.nacos;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlc.live.common.nacos.NacosConfigClient;
import com.tlc.live.toggle.config.ToggleProperties;
import com.tlc.live.toggle.event.TogglesChangedEvent;
import com.tlc.live.toggle.service.FeatureToggleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Nacos 同步组件（push 模式的服务端侧）。
 *
 * <p>启动时从 Nacos 拉取快照恢复状态（重启不丢配置）；
 * 本地任何开关变更实时发布到 Nacos；同时保持长轮询监听，
 * 接受外部（如 Nacos 控制台/其他系统）直接改配置的场景。
 */
@Component
public class NacosToggleSync implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NacosToggleSync.class);

    private final NacosConfigClient nacosClient;
    private final FeatureToggleService toggleService;
    private final ObjectMapper objectMapper;
    private final ToggleProperties props;
    private volatile boolean running = true;

    public NacosToggleSync(NacosConfigClient nacosClient, FeatureToggleService toggleService,
                           ObjectMapper objectMapper, ToggleProperties props) {
        this.nacosClient = nacosClient;
        this.toggleService = toggleService;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!props.isNacosEnabled()) {
            log.info("Nacos 同步未启用，toggle 仅本地内存模式");
            return;
        }
        Thread.ofVirtual().name("nacos-toggle-sync").start(this::listenLoop);
        try {
            String content = nacosClient.fetch();
            if (content != null) {
                toggleService.applySnapshot(objectMapper.readValue(content, new TypeReference<Map<String, Boolean>>() {}));
                log.info("已从 Nacos 恢复开关快照: {}", content);
            }
            log.info("Nacos 开关同步已启动");
        } catch (Exception ex) {
            log.warn("Nacos 启动拉取失败（进入重试监听）: {}", ex.getMessage());
        }
    }

    /** 本地变更 → 推送到 Nacos（异步，不阻塞开关请求）。 */
    @EventListener
    public void onTogglesChanged(TogglesChangedEvent event) {
        if (!props.isNacosEnabled()) {
            return;
        }
        Thread.ofVirtual().name("nacos-toggle-push").start(() -> {
            try {
                String json = objectMapper.writeValueAsString(event.snapshot());
                nacosClient.publish(json);
                log.debug("开关快照已推送 Nacos: {}", json);
            } catch (Exception ex) {
                log.warn("开关快照推送 Nacos 失败: {}", ex.getMessage());
            }
        });
    }

    private void listenLoop() {
        String lastContent = null;
        while (running) {
            try {
                String current = nacosClient.fetch();
                boolean changed = nacosClient.longPoll(NacosConfigClient.md5Hex(current), 25);
                if (changed) {
                    String content = nacosClient.fetch();
                    if (content != null && !content.equals(lastContent)) {
                        log.info("Nacos 配置变更: {}", content);
                        toggleService.applySnapshot(
                                objectMapper.readValue(content, new TypeReference<Map<String, Boolean>>() {}));
                    }
                    lastContent = content;
                } else {
                    lastContent = current;
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                log.warn("Nacos 长轮询异常，3 秒后重试: {}", ex.getMessage());
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }


}