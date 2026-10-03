package com.tlc.live.gateway.nacos;

import com.tlc.live.common.nacos.NacosConfigClient;
import com.tlc.live.gateway.toggle.ToggleSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Nacos 长轮询监听器：开关配置一变即刷新本地快照（推拉模型的"推"）。
 *
 * <p>Nacos 服务端会 hold 住请求最长 25 秒，变更近实时返回；
 * 网关热路径只读本地 volatile 快照，零网络开销。
 */
@Component
public class NacosToggleWatcher implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NacosToggleWatcher.class);

    private final NacosConfigClient nacosClient;
    private final ToggleSnapshot toggleSnapshot;
    private final ObjectMapper objectMapper;
    private volatile boolean running = true;

    public NacosToggleWatcher(NacosConfigClient nacosClient, ToggleSnapshot toggleSnapshot, ObjectMapper objectMapper) {
        this.nacosClient = nacosClient;
        this.toggleSnapshot = toggleSnapshot;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        Thread.ofVirtual().name("nacos-toggle-watcher").start(() -> {
            String lastContent = null;
            while (running) {
                try {
                    String current = nacosClient.fetch();
                    boolean changed = nacosClient.longPoll(NacosConfigClient.md5Hex(current), 25);
                    if (changed) {
                        String content = nacosClient.fetch();
                        if (content != null) {
                            Map<String, Boolean> snapshot =
                                    objectMapper.readValue(content, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Boolean>>() {});
                            toggleSnapshot.update(snapshot);
                            log.info("开关快照已刷新: {}", content);
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
        });
        log.info("Nacos 开关监听已启动");
    }
}