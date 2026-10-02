package com.tlc.live.seckill.web;

import com.tlc.live.seckill.model.DeductOutcome;
import com.tlc.live.seckill.service.RedPacketPrepareService;
import com.tlc.live.seckill.service.SecKillDeductService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 联调旁路：生产链路上 AI 决策与抢红包请求均走 RocketMQ，
 * 此接口仅供本地无 MQ 环境时手工触发同一套服务逻辑，便于端到端验证。
 */
@RestController
@RequestMapping("/api/seckill")
public class SeckillDevController {

    private final RedPacketPrepareService prepareService;
    private final SecKillDeductService deductService;

    public SeckillDevController(RedPacketPrepareService prepareService, SecKillDeductService deductService) {
        this.prepareService = prepareService;
        this.deductService = deductService;
    }

    /** 手工发红包雨，请求体：{"roomId":"room-1","totalFen":10000,"count":500} */
    @PostMapping("/drop")
    public Map<String, Object> drop(@RequestBody DropBody body) {
        long redPacketId = prepareService.prepare(body.roomId(), body.totalFen(), body.count());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("redPacketId", redPacketId);
        return result;
    }

    /** 手工抢红包，请求体：{"userId":10001,"roomId":"room-1","redPacketId":123} */
    @PostMapping("/rush")
    public Map<String, Object> rush(@RequestBody RushBody body) {
        DeductOutcome outcome = deductService.deduct(body.userId(), body.roomId(), body.redPacketId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", outcome.status());
        result.put("amountFen", outcome.amountFen());
        return result;
    }

    public record DropBody(String roomId, int totalFen, int count) {
    }

    public record RushBody(long userId, String roomId, long redPacketId) {
    }
}
