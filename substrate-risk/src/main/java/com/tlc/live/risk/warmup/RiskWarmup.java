package com.tlc.live.risk.warmup;

import com.tlc.live.risk.check.BlacklistCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 风控预热：启动时先建立 Redis 连接并触发一次指纹查询。
 * 50ms 判定窗口极紧，首个请求绝不能承受连接池冷启动的开销。
 */
@Component
public class RiskWarmup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RiskWarmup.class);

    private final StringRedisTemplate redis;
    private final BlacklistCheckService blacklistCheck;

    public RiskWarmup(StringRedisTemplate redis, BlacklistCheckService blacklistCheck) {
        this.redis = redis;
        this.blacklistCheck = blacklistCheck;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            blacklistCheck.check(0);
            log.info("风控连接池预热完成");
        } catch (Exception ex) {
            log.warn("风控预热失败（不影响启动）: {}", ex.getMessage());
        }
    }
}