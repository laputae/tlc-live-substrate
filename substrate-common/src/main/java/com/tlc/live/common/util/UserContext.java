package com.tlc.live.common.util;

/**
 * 虚拟线程安全的用户上下文。
 *
 * <p>使用 JDK 25 的 {@code ScopedValue} 替代 {@code ThreadLocal}：
 * 不可变、有作用域、随结构化并发子任务自动继承，不会被线程池复用污染，
 * 且在百万虚拟线程场景下无 ThreadLocal 的哈希开销。
 */
public final class UserContext {

    public static final ScopedValue<String> USER_TOKEN = ScopedValue.newInstance();
    public static final ScopedValue<Long> ROOM_ID = ScopedValue.newInstance();

    private UserContext() {
    }

    public static String token() {
        return USER_TOKEN.isBound() ? USER_TOKEN.get() : null;
    }

    public static Long roomId() {
        return ROOM_ID.isBound() ? ROOM_ID.get() : null;
    }

    /**
     * 绑定上下文后执行任务，作用域退出即自动解绑。
     */
    public static <T> T runWith(String token, Long roomId, ScopedValue.CallableOp<T, Exception> task) throws Exception {
        return ScopedValue.where(USER_TOKEN, token)
                .where(ROOM_ID, roomId == null ? 0L : roomId)
                .call(task);
    }
}
