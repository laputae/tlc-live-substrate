package com.tlc.live.seckill.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 轻量 ID 生成器：毫秒时间戳左移 12 位加序列号，单机够用。
 * 后续引入雪花集群时替换实现，调用方不感知。
 */
public final class IdGenerator {

    private static final AtomicLong SEQUENCE = new AtomicLong();

    private IdGenerator() {
    }

    public static long nextId() {
        long seq = SEQUENCE.incrementAndGet() & 0xFFF;
        return (System.currentTimeMillis() << 12) | seq;
    }
}
