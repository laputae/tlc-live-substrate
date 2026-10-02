package com.tlc.live.seckill.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlc.live.common.util.LuaScriptLoader;
import com.tlc.live.seckill.model.DeductOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 秒杀原子扣减：库存校验、幂等去重、凭证弹出、中奖记录在一个 Lua 脚本内完成，
 * 分布式环境下无锁且绝对不超卖（发 10000 个绝不会被 10001 个人抢到）。
 */
@Service
public class SecKillDeductService {

    private static final Logger log = LoggerFactory.getLogger(SecKillDeductService.class);

    private static final String LUA = "lua/seckill_deduct.lua";
    private static final String PERSIST_QUEUE = "rp:persist:queue";

    private final LuaScriptLoader luaLoader;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public SecKillDeductService(LuaScriptLoader luaLoader, StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.luaLoader = luaLoader;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public DeductOutcome deduct(long userId, String roomId, long redPacketId) {
        long result = luaLoader.execute(LUA,
                List.of("rp:stock:" + redPacketId, "rp:winners:" + redPacketId, "rp:dedup:" + redPacketId),
                String.valueOf(userId));

        // 回写已参与标记，供风控服务的"重复参与"预过滤使用
        try {
            redis.opsForSet().add("rp:rushed:" + redPacketId, String.valueOf(userId));
        } catch (Exception ex) {
            log.debug("已参与标记回写失败（不影响主流程）userId={} rpId={}", userId, redPacketId, ex);
        }

        if (result == 1) {
            return DeductOutcome.already();
        }
        if (result == 2) {
            return DeductOutcome.soldOut();
        }
        if (result <= 0) {
            log.error("Lua 扣减异常 userId={} redPacketId={} result={}", userId, redPacketId, result);
            return DeductOutcome.soldOut();
        }

        int amountFen = (int) result;
        onWin(userId, roomId, redPacketId, amountFen);
        return DeductOutcome.win(amountFen);
    }

    /** 抢中后的旁路动作：进持久化队列（MySQL 最终一致）+ 下行广播。 */
    private void onWin(long userId, String roomId, long redPacketId, int amountFen) {
        try {
            String record = objectMapper.writeValueAsString(Map.of(
                    "redPacketId", redPacketId,
                    "roomId", roomId,
                    "userId", userId,
                    "amountFen", amountFen));
            redis.opsForList().leftPush(PERSIST_QUEUE, record);
            redis.expire(PERSIST_QUEUE, Duration.ofDays(7));
        } catch (Exception ex) {
            log.error("中奖记录入持久化队列失败 userId={} redPacketId={}", userId, redPacketId, ex);
        }
        try {
            redis.convertAndSend(RedPacketPrepareService.DOWNSTREAM_CHANNEL,
                    objectMapper.writeValueAsString(Map.of(
                            "type", "REDPACKET_WIN",
                            "redPacketId", redPacketId,
                            "roomId", roomId,
                            "userId", userId,
                            "amountFen", amountFen)));
        } catch (Exception ex) {
            log.error("中奖结果广播失败 userId={} redPacketId={}", userId, redPacketId, ex);
        }
    }
}
