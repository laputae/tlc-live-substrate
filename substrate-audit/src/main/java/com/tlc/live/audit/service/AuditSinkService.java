package com.tlc.live.audit.service;

import com.tlc.live.audit.doc.AgentAuditDoc;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 审计落库管道。
 *
 * <p>采集必须异步：LLM 调用本身是低频高延迟链路，审计不能反过来拖慢业务。
 * 上报事件先进有界缓冲队列（队列满则丢弃并计数，审计优先于审计完整性保证业务），
 * 由独立虚拟线程批量刷入 Elasticsearch；ES 不可用时降级为本地日志，事件不丢进黑洞。
 */
@Service
public class AuditSinkService {

    private static final Logger log = LoggerFactory.getLogger(AuditSinkService.class);

    private final ElasticsearchOperations operations;
    private final boolean elasticsearchEnabled;
    private final int flushBatchSize;
    private final BlockingQueue<AgentAuditDoc> buffer = new ArrayBlockingQueue<>(10_000);
    private final AtomicLong dropped = new AtomicLong();
    private volatile boolean running = true;

    public AuditSinkService(ElasticsearchOperations operations,
                            @Value("${tlc.audit.elasticsearch-enabled:true}") boolean elasticsearchEnabled,
                            @Value("${tlc.audit.flush-batch-size:200}") int flushBatchSize) {
        this.operations = operations;
        this.elasticsearchEnabled = elasticsearchEnabled;
        this.flushBatchSize = flushBatchSize;
    }

    /** 提交审计事件（AI 中枢异步上报入口）。 */
    public boolean submit(AgentAuditDoc event) {
        return buffer.offer(event);
    }

    public long droppedCount() {
        return dropped.get();
    }

    public int backlog() {
        return buffer.size();
    }

    @PostConstruct
    public void start() {
        Thread.ofVirtual().name("audit-sink").start(this::drainLoop);
        log.info("审计落库管道已启动：ES={}，flushBatchSize={}", elasticsearchEnabled, flushBatchSize);
    }

    private void drainLoop() {
        List<AgentAuditDoc> batch = new ArrayList<>(flushBatchSize);
        while (running || !buffer.isEmpty()) {
            try {
                AgentAuditDoc first = buffer.poll(1, TimeUnit.SECONDS);
                if (first == null) {
                    continue;
                }
                batch.add(first);
                buffer.drainTo(batch, flushBatchSize - 1);
                flush(List.copyOf(batch));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            } finally {
                batch.clear();
            }
        }
    }

    private void flush(List<AgentAuditDoc> batch) {
        if (!elasticsearchEnabled) {
            batch.forEach(doc -> log.info("AUDIT(文件模式) {}", doc));
            return;
        }
        try {
            // DocumentOperations.save(Iterable, IndexCoordinates)：批量写入
            operations.save(batch, IndexCoordinates.of("agent-audit"));
        } catch (Exception ex) {
            // ES 不可用：降级为本地日志，保证审计链路可追踪、可重放
            log.error("审计事件写入 ES 失败({} 条)，降级输出本地日志: {}", batch.size(), ex.getMessage());
            batch.forEach(doc -> log.warn("AUDIT(降级) {}", doc));
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
    }
}
