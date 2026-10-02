package com.tlc.live.seckill.service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 红包金额预拆分：二倍均值法（微信红包同款算法）。
 *
 * <p>金额在发红包时一次性拆好并随凭证入队，抢的时候 LPOP 到哪个凭证
 * 金额就随之确定，抢的链路上不做任何金额计算，保持极致简单与强一致。
 */
public final class AmountAllocator {

    private AmountAllocator() {
    }

    /**
     * @param totalFen 总金额（分），必须 >= count
     * @param count    红包个数
     * @return 长度为 count 的金额数组（分），总和恒等于 totalFen，每份至少 1 分
     */
    public static int[] split(int totalFen, int count) {
        if (totalFen < count) {
            throw new IllegalArgumentException("总金额(分)必须不小于红包个数");
        }
        int[] amounts = new int[count];
        int remain = totalFen;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count - 1; i++) {
            // 剩余每人保底 1 分后，取剩余均值的两倍作为随机上限
            int safeRemain = remain - (count - 1 - i);
            int upper = safeRemain / (count - i) * 2;
            int amount = upper <= 1 ? 1 : random.nextInt(1, upper);
            amounts[i] = amount;
            remain -= amount;
        }
        amounts[count - 1] = remain;
        return amounts;
    }
}
