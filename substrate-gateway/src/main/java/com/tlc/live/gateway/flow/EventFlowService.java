package com.tlc.live.gateway.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlc.live.common.event.DanmakuEvent;
import com.tlc.live.common.event.RushRedPacketEvent;
import com.tlc.live.common.util.UserContext;
import com.tlc.live.gateway.client.RiskClient;
import com.tlc.live.gateway.config.GatewayProperties;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 上行事件处理流水线（DOP 路由的执行端）。
 *
 * <p>RushRedPacketEvent：调风控短路查杀，放行后投递 tlc-seckill-rush 削峰；
 * DanmakuEvent：纯互动流量，投递 tlc-agent-danmaku 给 AI 中枢做情绪聚合。
 * 两条链路物理隔离，弹幕洪峰永远不会冲击交易通道。
 */
@Service
public class EventFlowService {

    private static final Logger log = LoggerFactory.getLogger(EventFlowService.class);

    private final RiskClient riskClient;
    private final RocketMQTemplate rocketMQTemplate;
    private final GatewayProperties props;
    private final ObjectMapper objectMapper;

    public EventFlowService(RiskClient riskClient, RocketMQTemplate rocketMQTemplate,
                            GatewayProperties props, ObjectMapper objectMapper) {
        this.riskClient = riskClient;
        this.rocketMQTemplate = rocketMQTemplate;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    /** 抢红包事件：风控放行后入 MQ。 */
    public Map<String, Object> handleRush(RushRedPacketEvent event) {
        try {
            // ScopedValue 绑定用户令牌，风控调用透传
            return UserContext.runWith("u" + event.userId(), null, () -> doHandleRush(event));
        } catch (Exception ex) {
            log.error("抢红包事件处理失败 userId={} rpId={}", event.userId(), event.redPacketId(), ex);
            return Map.of("risk", "ERROR", "reason", "内部异常");
        }
    }

    private Map<String, Object> doHandleRush(RushRedPacketEvent event) throws Exception {
        RiskClient.RiskCheckResult result = riskClient.check(event.userId(), event.roomId(), event.redPacketId());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("risk", result.passed() ? "PASS" : "REJECT");
        if (!result.passed()) {
            response.put("reason", result.reason());
            return response;
        }
        String payload = objectMapper.writeValueAsString(event);
        rocketMQTemplate.syncSend(props.getRushTopic(), payload);
        response.put("queued", true);
        return response;
    }

    /** 弹幕事件：转投 AI 中枢，无任何交易语义。 */
    public void handleDanmaku(DanmakuEvent event) {
        try {
            rocketMQTemplate.syncSend(props.getDanmakuTopic(), objectMapper.writeValueAsString(event));
        } catch (Exception ex) {
            log.warn("弹幕投递失败（可容忍丢失）: {}", ex.getMessage());
        }
    }
}
