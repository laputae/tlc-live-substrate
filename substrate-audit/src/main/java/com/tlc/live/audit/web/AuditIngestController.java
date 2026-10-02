package com.tlc.live.audit.web;

import com.tlc.live.audit.doc.AgentAuditDoc;
import com.tlc.live.audit.service.AuditSinkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 审计采集入口：AI 中枢在每次 LLM 调用结束后异步上报。
 */
@RestController
@RequestMapping("/api/audit")
public class AuditIngestController {

    private final AuditSinkService sinkService;

    public AuditIngestController(AuditSinkService sinkService) {
        this.sinkService = sinkService;
    }

    @PostMapping("/ingest")
    public Map<String, Object> ingest(@RequestBody AgentAuditDoc event) {
        boolean accepted = sinkService.submit(event);
        return Map.of("accepted", accepted);
    }

    @PostMapping("/ingest/batch")
    public Map<String, Object> ingestBatch(@RequestBody List<AgentAuditDoc> events) {
        long accepted = events.stream().filter(sinkService::submit).count();
        return Map.of("accepted", accepted, "rejected", events.size() - accepted);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("backlog", sinkService.backlog(), "dropped", sinkService.droppedCount());
    }
}
