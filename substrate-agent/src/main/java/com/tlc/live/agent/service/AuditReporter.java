package com.tlc.live.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 审计上报器：把每次 LLM 调用的输入、输出、Token 消耗异步报送审计服务，
 * 解决"AI 自主发了 10 万红包，财务不知道为什么"的黑盒问题。
 */
@Service
public class AuditReporter {

    private static final Logger log = LoggerFactory.getLogger(AuditReporter.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AuditReporter(ObjectMapper objectMapper,
                         @Value("${tlc.agent.audit-base-url:http://localhost:8086}") String auditBaseUrl) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(auditBaseUrl).build();
    }

    /** 异步上报（虚拟线程），绝不阻塞决策主链路。 */
    public void report(String roomId, String agentName, String prompt, String output,
                       Integer promptTokens, Integer completionTokens, long costMs, String decision) {
        Thread.ofVirtual().name("audit-report").start(() -> doReport(
                roomId, agentName, prompt, output, promptTokens, completionTokens, costMs, decision));
    }

    private void doReport(String roomId, String agentName, String prompt, String output,
                          Integer promptTokens, Integer completionTokens, long costMs, String decision) {
        try {
            Map<String, Object> event = new java.util.LinkedHashMap<>();
            event.put("eventId", UUID.randomUUID().toString());
            event.put("traceId", UUID.randomUUID().toString());
            event.put("roomId", roomId == null ? "" : roomId);
            event.put("agentName", agentName);
            event.put("prompt", prompt);
            event.put("output", output);
            event.put("promptTokens", java.util.Objects.requireNonNullElse(promptTokens, 0));
            event.put("completionTokens", java.util.Objects.requireNonNullElse(completionTokens, 0));
            event.put("costMs", costMs);
            event.put("decision", decision);
            event.put("createdAt", Instant.now().toString());
            restClient.post()
                    .uri("/api/audit/ingest")
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsString(event))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("审计上报失败（不影响业务）: {}", ex.getMessage());
        }
    }
}
