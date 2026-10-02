package com.tlc.live.push.web;

import com.tlc.live.push.handler.LiveBroadcastHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 运维/联调入口：网关与秒杀服务最终也应通过 Redis Pub/Sub 触达推送集群，
 * 此接口仅用于压测观测与人工注入下行消息。
 */
@RestController
@RequestMapping("/api/push")
public class PushAdminController {

    private final LiveBroadcastHandler broadcastHandler;

    public PushAdminController(LiveBroadcastHandler broadcastHandler) {
        this.broadcastHandler = broadcastHandler;
    }

    @GetMapping("/online")
    public Map<String, Object> online() {
        return Map.of("online", broadcastHandler.onlineCount());
    }

    @PostMapping("/broadcast")
    public Map<String, Object> broadcast(@RequestBody String payload) {
        broadcastHandler.broadcast(payload);
        return Map.of("broadcast", true, "online", broadcastHandler.onlineCount());
    }
}
