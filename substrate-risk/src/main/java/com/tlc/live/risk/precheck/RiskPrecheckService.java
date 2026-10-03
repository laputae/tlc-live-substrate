package com.tlc.live.risk.precheck;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.tlc.live.common.util.LuaScriptLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 风控画像缓存与预检：黑名单 + 信誉分合并为一次 EVALSHA 预检，
 * 结果进 Caffeine 本地缓存（黑名单/信誉分统一 TTL 30s）。
 *
 * 并发要点：使用 LoadingCache.get(key, loader)——同一 userId 并发未命中
 * 只触发一次加载，不同 userId 完全并行，无全局锁（避免 synchronized
 * 把全部 miss 串行化导致 50ms 窗口内大面积熔断）。
 * Redis 不可用时降级为"非黑名单 + 满信誉"（fail-open），且降级值不写缓存。
 */
@Service
public class RiskPrecheckService {

    public record RiskProfile(boolean blacklisted, int score) {
    }

    private static final Logger log = LoggerFactory.getLogger(RiskPrecheckService.class);

    private static final String PRECHECK_LUA = "lua/risk_precheck.lua";

    private final LuaScriptLoader luaLoader;
    private final com.github.benmanes.caffeine.cache.LoadingCache<Long, RiskProfile> cache =
            Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofSeconds(30))
                    .maximumSize(200_000)
                    .build(this::loadProfile);

    public RiskPrecheckService(LuaScriptLoader luaLoader) {
        this.luaLoader = luaLoader;
    }

    public boolean isBlacklisted(long userId) {
        RiskProfile profile = cache.get(userId, this::loadProfile);
        return profile != null && profile.blacklisted();
    }

    public int creditScore(long userId) {
        RiskProfile profile = cache.get(userId, this::loadProfile);
        return profile != null ? profile.score() : 100;
    }

    /** 单次 EVALSHA 同时取黑名单与信誉分；失败返回 null（不缓存，下次重试）。 */
    private RiskProfile loadProfile(long userId) {
        try {
            List<String> res = luaLoader.executeForList(PRECHECK_LUA,
                    List.of("risk:blacklist", "risk:credit"), String.valueOf(userId));
            return new RiskProfile(
                    "1".equals(res.get(0)),
                    res.get(1) == null ? 100 : Integer.parseInt(res.get(1)));
        } catch (Exception ex) {
            log.warn("风控画像加载失败 userId={}: {}", userId, ex.getMessage());
            return null;
        }
    }
}