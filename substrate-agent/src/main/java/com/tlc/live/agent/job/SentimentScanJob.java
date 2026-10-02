package com.tlc.live.agent.job;

import com.tlc.live.agent.agent.ActionAgent;
import com.tlc.live.agent.agent.SentimentAgent;
import com.tlc.live.agent.config.AgentProperties;
import com.tlc.live.agent.model.RedPacketDecision;
import com.tlc.live.agent.service.AuditReporter;
import com.tlc.live.agent.service.DanmakuWindowService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 情绪扫描调度：周期性对各房间弹幕窗口做 Multi-Agent 决策。
 *
 * <p>SentimentAgent 判定冷场 → ActionAgent 决定红包参数（预算围栏内）→
 * 决策事件投递 tlc-agent-decision，由秒杀服务预热执行。LLM 全程异步、
 * 低频运行，与秒杀的极速链路物理隔离。
 */
@Component
public class SentimentScanJob {

    private static final Logger log = LoggerFactory.getLogger(SentimentScanJob.class);

    private static final String DECISION_TOPIC = "tlc-agent-decision";

    private final DanmakuWindowService windowService;
    private final SentimentAgent sentimentAgent;
    private final ActionAgent actionAgent;
    private final AuditReporter auditReporter;
    private final RocketMQTemplate rocketMQTemplate;
    private final AgentProperties props;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public SentimentScanJob(DanmakuWindowService windowService, SentimentAgent sentimentAgent,
                            ActionAgent actionAgent, AuditReporter auditReporter,
                            RocketMQTemplate rocketMQTemplate, AgentProperties props,
                            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.windowService = windowService;
        this.sentimentAgent = sentimentAgent;
        this.actionAgent = actionAgent;
        this.auditReporter = auditReporter;
        this.rocketMQTemplate = rocketMQTemplate;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${tlc.agent.scan-interval-ms:15000}")
    public void scan() {
        for (String roomId : windowService.rooms().keySet()) {
            if (windowService.pending(roomId) < props.getMinSamples()) {
                continue;
            }
            List<String> sample = windowService.drain(roomId);
            long start = System.currentTimeMillis();

            SentimentAgent.Mood mood = sentimentAgent.analyze(roomId, sample);
            log.info("情绪分析 roomId={} mood={} score={}", roomId, mood.mood(), mood.score());
            auditReporter.report(roomId, "SentimentAgent", "samples=" + sample.size(),
                    mood.mood() + "/" + mood.score(), null, null,
                    System.currentTimeMillis() - start, "分析完成");

            boolean cold = "COLD".equals(mood.mood()) || mood.score() < props.getColdThreshold();
            if (!cold) {
                continue;
            }

            long decisionStart = System.currentTimeMillis();
            ActionAgent.Decision decision = actionAgent.decide(roomId, mood, sample);
            if (!decision.shouldDrop()) {
                continue;
            }
            try {
                RedPacketDecision event = RedPacketDecision.drop(roomId, decision.totalFen(), decision.count());
                rocketMQTemplate.syncSend(DECISION_TOPIC, objectMapper.writeValueAsString(event));
                log.info("AI 决策下发红包雨 roomId={} totalFen={} count={} reason={}",
                        roomId, decision.totalFen(), decision.count(), decision.reason());
                auditReporter.report(roomId, "ActionAgent", "mood=" + mood.mood() + "/" + mood.score(),
                        objectMapper.writeValueAsString(event), null, null,
                        System.currentTimeMillis() - decisionStart, decision.reason());
            } catch (Exception ex) {
                log.error("决策事件投递失败 roomId={}", roomId, ex);
            }
        }
    }
}
