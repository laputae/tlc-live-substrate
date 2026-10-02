package com.tlc.live.seckill.model;

/**
 * 原子扣减结果。
 *
 * @param status    WIN / ALREADY / SOLD_OUT
 * @param amountFen 抢中金额（分），仅 WIN 时有效
 */
public record DeductOutcome(Status status, int amountFen) {

    public enum Status { WIN, ALREADY, SOLD_OUT }

    public static DeductOutcome win(int amountFen) {
        return new DeductOutcome(Status.WIN, amountFen);
    }

    public static DeductOutcome already() {
        return new DeductOutcome(Status.ALREADY, 0);
    }

    public static DeductOutcome soldOut() {
        return new DeductOutcome(Status.SOLD_OUT, 0);
    }
}
