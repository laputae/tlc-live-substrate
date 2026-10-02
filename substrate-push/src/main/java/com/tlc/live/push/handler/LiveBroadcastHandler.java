package com.tlc.live.push.handler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 高密下行广播处理器。
 *
 * <p>本集群纯无状态：连接挂在哪台实例，就从 Redis Pub/Sub 订阅同一渠道，
 * 横向扩容不会产生重复推送。单实例百万连接场景下，广播应进一步分片到
 * 专用写线程池（虚拟线程）并合并下行帧，这里保持最简同步写。
 */
@Component
public class LiveBroadcastHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LiveBroadcastHandler.class);

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    /** 当前实例在线连接数（供运维探针与压测观测）。 */
    public int onlineCount() {
        return sessions.size();
    }

    /** 向本实例全部在线连接广播一条下行消息。 */
    public void broadcast(String payload) {
        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                sessions.remove(session);
                continue;
            }
            try {
                // 同一 session 的写必须串行，加锁防止帧交错
                synchronized (session) {
                    session.sendMessage(message);
                }
            } catch (IOException ex) {
                log.warn("下行推送失败，剔除连接 {}: {}", session.getId(), ex.getMessage());
                sessions.remove(session);
            }
        }
    }
}
