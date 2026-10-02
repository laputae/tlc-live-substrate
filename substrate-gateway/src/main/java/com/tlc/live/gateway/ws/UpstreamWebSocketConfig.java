package com.tlc.live.gateway.ws;

import com.tlc.live.gateway.config.GatewayProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 上行长连接注册：弹幕与抢红包请求的上行入口。
 */
@Configuration
@EnableWebSocket
public class UpstreamWebSocketConfig implements WebSocketConfigurer {

    private final UpstreamEventHandler handler;
    private final GatewayProperties props;

    public UpstreamWebSocketConfig(UpstreamEventHandler handler, GatewayProperties props) {
        this.handler = handler;
        this.props = props;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, props.getWsPath()).setAllowedOriginPatterns("*");
    }
}
