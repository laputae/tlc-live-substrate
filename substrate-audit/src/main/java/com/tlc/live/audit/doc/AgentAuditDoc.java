package com.tlc.live.audit.doc;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

/**
 * AI 智能体审计单据：解决"AI 黑盒化"问题。
 *
 * <p>财务/合规要能回答：AI 为什么发红包？基于什么情绪指标？消耗了多少 Token？
 * 该单据由 AI 中枢在每次 LLM 调用后异步上报。
 */
@Document(indexName = "agent-audit")
public record AgentAuditDoc(
        @Id String eventId,
        String traceId,
        String roomId,
        String agentName,
        String prompt,
        String output,
        Integer promptTokens,
        Integer completionTokens,
        Long costMs,
        String decision,
        Instant createdAt) {
}
