package com.tlc.live.common.util;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

/**
 * Redis Lua 脚本加载器。
 *
 * <p>秒杀链路将「库存校验 + 余额扣减 + 中奖名单写入」合并为单脚本原子执行，
 * 脚本内容在应用启动时一次性读入并缓存，避免高频 I/O。
 */
@Component
public class LuaScriptLoader {

    private final StringRedisTemplate redisTemplate;
    private final Map<String, DefaultRedisScript<Long>> scriptCache = new ConcurrentHashMap<>();

    public LuaScriptLoader(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 执行 classpath 下的 Lua 脚本。
     *
     * @param location classpath 路径，如 {@code lua/seckill_deduct.lua}
     * @param keys     KEYS 列表
     * @param args     ARGV 列表
     * @return 脚本返回的 long 结果
     */
    public long execute(String location, java.util.List<String> keys, Object... args) {
        DefaultRedisScript<Long> script = scriptCache.computeIfAbsent(location, this::load);
        Long result = redisTemplate.execute(script, keys, args);
        return result == null ? -1L : result;
    }

    private DefaultRedisScript<Long> load(String location) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText(read(location));
        return script;
    }

    private String read(String location) {
        try (InputStreamReader reader =
                     new InputStreamReader(new ClassPathResource(location).getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (Exception ex) {
            throw new IllegalStateException("加载 Lua 脚本失败: " + location, ex);
        }
    }
}
