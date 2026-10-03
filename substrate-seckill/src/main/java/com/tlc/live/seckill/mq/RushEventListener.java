package com.tlc.live.seckill.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlc.live.seckill.model.DeductOutcome;
import com.tlc.live.seckill.model.RushRequest;
import com.tlc.live.seckill.service.SecKillDeductService;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 抢红包请求消费者：只接收风控放行的请求，MQ 已把 10 万瞬时点击削平为
 * 稳定的队列消费流；这里做 Lua 原子扣减（极速 + 强一致）。
 */
@Component
@RocketMQMessageListener(
        topic = "tlc-seckill-rush",
        consumerGroup = "tlc-seckill-deduct-consumer",
        consumeMode = ConsumeMode.CONCURRENTLY,
        consumeThreadMax = 200)
public class RushEventListener implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(RushEventListener.class);

    private final SecKillDeductService deductService;
    private final ObjectMapper objectMapper;

    public RushEventListener(SecKillDeductService deductService, ObjectMapper objectMapper) {
        this.deductService = deductService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(String message) {
        try {
            RushRequest request = objectMapper.readValue(message, RushRequest.class);
            DeductOutcome outcome = deductService.deduct(request.userId(), request.roomId(), request.redPacketId());
            if (outcome.status() == DeductOutcome.Status.WIN) {
                log.info("抢中 userId={} redPacketId={} amountFen={}",
                        request.userId(), request.redPacketId(), outcome.amountFen());
            }
        } catch (Exception ex) {
            log.error("抢红包消费失败，进入重试: {} error={}", message, ex.getMessage(), ex);
            throw new RuntimeException("consume failed, will retry", ex);
        }
    }
}
