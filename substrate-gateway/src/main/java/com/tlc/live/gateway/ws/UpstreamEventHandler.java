package com.tlc.live.gateway.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlc.live.common.event.DanmakuEvent;
import com.tlc.live.common.event.RushRedPacketEvent;
import com.tlc.live.gateway.flow.EventFlowService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 上行长连接处理器：百万终端的弹幕 / 抢红包请求都从这里进入。
 *
 * <p>上行报文 JSON 契约：
 * {"type":"DANMAKU","userId":1,"roomId":"room-1","content":"主播666"}
 * {"type":"RUSH","userId":1,"roomId":"room-1","redPacketId":123}
 */
@Component
public class UpstreamEventHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final EventFlowService eventFlow;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public UpstreamEventHandler(ObjectMapper objectMapper, EventFlowService eventFlow) {
        this.objectMapper = objectMapper;
        this.eventFlow = eventFlow;
    }

    public int onlineCount() {
        return sessions.size();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            String type = node.path("type").asText();
            long userId = node.path("userId").asLong();
            String roomId = node.path("roomId").asText();
            switch (type) {
                case "DANMAKU" -> eventFlow.handleDanmaku(
                        new DanmakuEvent(userId, roomId, node.path("content").asText()));
                case "RUSH" -> {
                    Map<String, Object> result = eventFlow.handleRush(
                            new RushRedPacketEvent(userId, roomId, node.path("redPacketId").asLong()));
                    safeSend(session, objectMapper.writeValueAsString(
                            Map.of("type", "RUSH_ACK", "result", result)));
                }
                default -> safeSend(session, "{\"type\":\"ERROR\",\"reason\":\"unknown type\"}");
            }
        } catch (IOException ex) {
            safeSend(session, "{\"type\":\"ERROR\",\"reason\":\"bad payload\"}");
            // 上行报文不合法或回执写失败：拒绝该帧但不断开连接
        }
    }

    private void safeSend(WebSocketSession session, String payload) {
        try {
            if (session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(new TextMessage(payload));
                }
            }
        } catch (Exception ignored) {
            // 下行 ack 失败不影响上行主流程
        }
    }
}
