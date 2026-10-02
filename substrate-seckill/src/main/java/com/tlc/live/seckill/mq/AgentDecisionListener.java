package com.tlc.live.seckill.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlc.live.seckill.model.RedPacketDropEvent;
import com.tlc.live.seckill.service.RedPacketPrepareService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 消费 AI 多智能体的场控决策（业务低频、高延迟链路的出口），
 * 触发红包雨预热：Redis 初始化凭证 + 广播红包出现。
 */
@Component
@RocketMQMessageListener(topic = "tlc-agent-decision", consumerGroup = "tlc-seckill-drop-consumer")
public class AgentDecisionListener implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(AgentDecisionListener.class);

    private final RedPacketPrepareService prepareService;
    private final ObjectMapper objectMapper;

    public AgentDecisionListener(RedPacketPrepareService prepareService, ObjectMapper objectMapper) {
        this.prepareService = prepareService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(String message) {
        try {
            RedPacketDropEvent event = objectMapper.readValue(message, RedPacketDropEvent.class);
            if (!RedPacketDropEvent.ACTION_DROP.equals(event.action())) {
                log.warn("忽略未知的 AI 决策动作: {}", event.action());
                return;
            }
            prepareService.prepare(event);
        } catch (Exception ex) {
            log.error("AI 决策消费失败，进入重试: {} error={}", message, ex.getMessage(), ex);
            throw new RuntimeException("consume failed, will retry", ex);
        }
    }
}
