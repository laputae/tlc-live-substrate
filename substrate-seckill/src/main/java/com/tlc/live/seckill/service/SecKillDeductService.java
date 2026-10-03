package com.tlc.live.seckill.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlc.live.common.util.LuaScriptLoader;
import com.tlc.live.seckill.model.DeductOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 秒杀原子扣减：幂等去重、凭证弹出、中奖记录、已参与标记、持久化入队
 * 在一个 Lua 脚本内原子完成——扣减路径仅 2 次 Redis 往返（EVALSHA + PUBLISH），
 * 分布式环境下无锁且绝对不超卖（发 10000 个绝不会被 10001 个人抢到）。
 */
@Service
public class SecKillDeductService {

    private static final Logger log = LoggerFactory.getLogger(SecKillDeductService.class);

    private static final String LUA = "lua/seckill_deduct.lua";

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
                List.of("rp:stock:" + redPacketId, "rp:winners:" + redPacketId, "rp:dedup:" + redPacketId,
                        "rp:rushed:" + redPacketId, "rp:persist:queue"),
                String.valueOf(userId), roomId, String.valueOf(redPacketId));

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
        publishWin(userId, roomId, redPacketId, amountFen);
        return DeductOutcome.win(amountFen);
    }

    /** 抢中后的旁路动作：下行广播（持久化入队已在 Lua 内完成，MySQL 最终一致）。 */
    private void publishWin(long userId, String roomId, long redPacketId, int amountFen) {
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