package com.tlc.live.push.config;

import com.tlc.live.push.handler.LiveBroadcastHandler;
import com.tlc.live.push.listener.DownstreamEventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class PushConfig implements WebSocketConfigurer {

    private final LiveBroadcastHandler broadcastHandler;

    public PushConfig(LiveBroadcastHandler broadcastHandler) {
        this.broadcastHandler = broadcastHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(broadcastHandler, "/ws/live").setAllowedOriginPatterns("*");
    }

    /**
     * 订阅下行渠道：网关与秒杀服务把结果发布到 Redis Pub/Sub，
     * 推送集群每个实例各自订阅，实现内外网数据解耦与无限横向扩容。
     */
    @Bean
    public RedisMessageListenerContainer downstreamListenerContainer(
            RedisConnectionFactory connectionFactory, DownstreamEventListener listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listener, new ChannelTopic("tlc:downstream:push"));
        return container;
    }
}
