package com.tlc.live.seckill.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 中奖记录批量落库：Redis 队列 → MySQL，实现最终一致。
 *
 * <p>抢中瞬间只写 Redis（极速），落库由后台调度低峰异步批量完成。
 * MySQL 表 DDL（幂等依赖唯一键）：
 * CREATE TABLE t_redpacket_record (
 *   id BIGINT AUTO_INCREMENT PRIMARY KEY,
 *   room_id VARCHAR(64) NOT NULL,
 *   user_id BIGINT NOT NULL,
 *   red_packet_id BIGINT NOT NULL,
 *   amount_fen INT NOT NULL,
 *   status VARCHAR(16) NOT NULL DEFAULT 'WIN',
 *   created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
 *   archived TINYINT NOT NULL DEFAULT 0,
 *   UNIQUE KEY uk_rp_user (red_packet_id, user_id)
 * ) ENGINE=InnoDB;
 */
@Component
public class WinRecordPersistJob {

    private static final Logger log = LoggerFactory.getLogger(WinRecordPersistJob.class);

    private static final String PERSIST_QUEUE = "rp:persist:queue";
    private static final String INSERT_SQL =
            "INSERT IGNORE INTO t_redpacket_record (room_id, user_id, red_packet_id, amount_fen, status) "
                    + "VALUES (?, ?, ?, ?, 'WIN')";

    private final StringRedisTemplate redis;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final int batchSize;

    public WinRecordPersistJob(StringRedisTemplate redis, JdbcTemplate jdbcTemplate,
                               ObjectMapper objectMapper,
                               @Value("${tlc.seckill.persist-batch-size:500}") int batchSize) {
        this.redis = redis;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${tlc.seckill.persist-interval-ms:1000}")
    public void persist() {
        List<JsonNode> batch = new ArrayList<>(batchSize);
        while (batch.size() < batchSize) {
            String raw = redis.opsForList().rightPop(PERSIST_QUEUE);
            if (raw == null) {
                break;
            }
            batch.add(parse(raw));
        }
        if (batch.isEmpty()) {
            return;
        }
        try {
            jdbcTemplate.batchUpdate(INSERT_SQL, batch, batch.size(), (ps, node) -> {
                ps.setString(1, node.path("roomId").asText());
                ps.setLong(2, node.path("userId").asLong());
                ps.setLong(3, node.path("redPacketId").asLong());
                ps.setInt(4, node.path("amountFen").asInt());
            });
            log.debug("中奖记录落库 {} 条", batch.size());
        } catch (Exception ex) {
            log.error("中奖记录落库失败，回滚队列稍后重试({} 条): {}", batch.size(), ex.getMessage());
            for (JsonNode node : batch) {
                redis.opsForList().leftPush(PERSIST_QUEUE, node.toString());
            }
        }
    }

    private JsonNode parse(Object raw) {
        try {
            return objectMapper.readTree(raw.toString());
        } catch (Exception ex) {
            throw new IllegalStateException("坏数据: " + raw, ex);
        }
    }
}
