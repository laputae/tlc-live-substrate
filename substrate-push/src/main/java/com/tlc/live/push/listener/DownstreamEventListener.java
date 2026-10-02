package com.tlc.live.push.listener;

import com.tlc.live.push.handler.LiveBroadcastHandler;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 下行事件监听器：收到 Redis Pub/Sub 消息即广播给本实例全部在线连接。
 */
@Component
public class DownstreamEventListener implements MessageListener {

    private final LiveBroadcastHandler broadcastHandler;

    public DownstreamEventListener(LiveBroadcastHandler broadcastHandler) {
        this.broadcastHandler = broadcastHandler;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        broadcastHandler.broadcast(payload);
    }
}
