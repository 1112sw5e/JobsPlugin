package com.tradewind.profession;

/** Result of counting one profession action. */
public record Reward(boolean counted, double money, long xp) {

    public static final Reward NONE = new Reward(false, 0.0D, 0L);
}

