package com.tlc.live.seckill.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlc.live.seckill.model.RedPacketDropEvent;
import com.tlc.live.seckill.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * 红包雨预热：消费 AI 决策后在 Redis 中初始化库存凭证。
 *
 * <p>金额按二倍均值法预拆分，凭证（ticketId:amountFen）LPUSH 进库存队列；
 * 之后所有抢夺只做 LPOP，天然无并发超卖。同时广播"红包雨出现"引导用户点击。
 */
@Service
public class RedPacketPrepareService {

    private static final Logger log = LoggerFactory.getLogger(RedPacketPrepareService.class);

    public static final String DOWNSTREAM_CHANNEL = "tlc:downstream:push";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedPacketPrepareService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public long prepare(String roomId, int totalFen, int count) {
        if (totalFen < count || count <= 0) {
            throw new IllegalArgumentException("totalFen 必须不小于 count 且 count > 0");
        }
        long redPacketId = IdGenerator.nextId();
        int[] amounts = AmountAllocator.split(totalFen, count);
        String stockKey = "rp:stock:" + redPacketId;
        byte[] stockKeyBytes = stockKey.getBytes(StandardCharsets.UTF_8);

        // 管道一次性写入全部凭证，避免 count 次网络往返
        redis.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            for (int i = 0; i < count; i++) {
                // ticket 三段式编码：redPacketId:seq:amountFen，全部为纯数字
                String ticket = redPacketId + ":" + i + ":" + amounts[i];
                connection.listCommands().lPush(stockKeyBytes, ticket.getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });

        // 元数据与过期时间
        redis.opsForHash().putAll("rp:meta:" + redPacketId, Map.of(
                "roomId", roomId,
                "totalFen", String.valueOf(totalFen),
                "count", String.valueOf(count)));
        redis.expire("rp:meta:" + redPacketId, Duration.ofDays(1));
        redis.expire(stockKey, Duration.ofDays(1));

        publishDownstream(Map.of(
                "type", "REDPACKET_DROP",
                "redPacketId", redPacketId,
                "roomId", roomId,
                "count", count));

        log.info("红包雨预热完成 redPacketId={} roomId={} count={} totalFen={}", redPacketId, roomId, count, totalFen);
        return redPacketId;
    }

    /** 从 AI 决策事件构造预热请求。 */
    public long prepare(RedPacketDropEvent event) {
        return prepare(event.roomId(), event.totalFen(), event.count());
    }

    public void publishDownstream(Map<?, ?> payload) {
        try {
            redis.convertAndSend(DOWNSTREAM_CHANNEL, objectMapper.writeValueAsString(payload));
        } catch (Exception ex) {
            log.error("下行广播发布失败: {}", ex.getMessage(), ex);
        }
    }
}
