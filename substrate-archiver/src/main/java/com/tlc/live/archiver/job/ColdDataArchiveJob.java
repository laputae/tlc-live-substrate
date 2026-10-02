package com.tlc.live.archiver.job;

import com.tlc.live.archiver.config.ArchiveProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * 冷热数据沉淀任务。
 *
 * <p>凌晨低谷期把 MySQL 中已结算的红包记录批量搬到 ClickHouse（列式存储），
 * 随后标记 archived=1 释放 OLTP 压力；次日运营的"互动转化率图谱""弹幕热词云"
 * 等百亿级报表在 ClickHouse 上秒级完成。
 *
 * <p>源表 DDL（由秒杀服务维护）：
 * t_redpacket_record(id BIGINT PK, room_id, user_id, red_packet_id,
 *                    amount_fen INT, status VARCHAR, created_at DATETIME, archived TINYINT)
 * 目标表 DDL（ClickHouse）：
 * CREATE TABLE tlc_dw.dw_redpacket_record (
 *   id UInt64, room_id UInt64, user_id UInt64, red_packet_id UInt64,
 *   amount_fen UInt32, status String, created_at DateTime, archived_at DateTime
 * ) ENGINE = MergeTree ORDER BY (room_id, created_at);
 */
@Component
public class ColdDataArchiveJob {

    private static final Logger log = LoggerFactory.getLogger(ColdDataArchiveJob.class);

    private static final String SELECT_SQL =
            "SELECT id, room_id, user_id, red_packet_id, amount_fen, status, created_at "
                    + "FROM t_redpacket_record WHERE archived = 0 ORDER BY id LIMIT ?";

    private static final String INSERT_CLICKHOUSE_SQL =
            "INSERT INTO dw_redpacket_record "
                    + "(id, room_id, user_id, red_packet_id, amount_fen, status, created_at, archived_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, now())";

    private static final String MARK_ARCHIVED_SQL =
            "UPDATE t_redpacket_record SET archived = 1 WHERE id = ?";

    private final ArchiveProperties props;
    private final JdbcTemplate source;
    private final JdbcTemplate target;

    public ColdDataArchiveJob(ArchiveProperties props, JdbcTemplate sourceJdbcTemplate,
                              JdbcTemplate targetJdbcTemplate) {
        this.props = props;
        this.source = sourceJdbcTemplate;
        this.target = targetJdbcTemplate;
    }

    @Scheduled(cron = "${tlc.archive.cron:0 0 3 * * ?}")
    public void archive() {
        if (!props.isEnabled()) {
            return;
        }
        long start = System.currentTimeMillis();
        int total = 0;
        List<Map<String, Object>> rows;
        while (!(rows = source.queryForList(SELECT_SQL, props.getBatchSize())).isEmpty()) {
            target.batchUpdate(INSERT_CLICKHOUSE_SQL, rows, rows.size(), (ps, row) -> {
                ps.setLong(1, ((Number) row.get("id")).longValue());
                ps.setLong(2, ((Number) row.get("room_id")).longValue());
                ps.setLong(3, ((Number) row.get("user_id")).longValue());
                ps.setLong(4, ((Number) row.get("red_packet_id")).longValue());
                ps.setInt(5, ((Number) row.get("amount_fen")).intValue());
                ps.setString(6, String.valueOf(row.get("status")));
                ps.setTimestamp(7, Timestamp.from(((Timestamp) row.get("created_at")).toInstant()));
            });
            source.batchUpdate(MARK_ARCHIVED_SQL, rows, rows.size(),
                    (ps, row) -> ps.setLong(1, ((Number) row.get("id")).longValue()));
            total += rows.size();
        }
        if (total > 0) {
            log.info("冷数据沉淀完成：{} 行，耗时 {} ms", total, System.currentTimeMillis() - start);
        }
    }
}
