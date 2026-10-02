package com.tlc.live.agent.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlc.live.agent.service.DanmakuWindowService;
import com.tlc.live.common.event.DanmakuEvent;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 消费网关投递的弹幕事件，进入房间滑动窗口等待情绪分析。
 */
@Component
@RocketMQMessageListener(topic = "tlc-agent-danmaku", consumerGroup = "tlc-agent-danmaku-consumer")
public class DanmakuListener implements RocketMQListener<String> {

    private final DanmakuWindowService windowService;
    private final ObjectMapper objectMapper;

    public DanmakuListener(DanmakuWindowService windowService, ObjectMapper objectMapper) {
        this.windowService = windowService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(String message) {
        try {
            DanmakuEvent event = objectMapper.readValue(message, DanmakuEvent.class);
            windowService.add(event.roomId(), event.content());
        } catch (Exception ex) {
            // 弹幕属于可丢失流量，解析失败直接吞掉
        }
    }
}
