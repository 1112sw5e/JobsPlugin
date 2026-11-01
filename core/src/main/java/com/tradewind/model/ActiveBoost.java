package com.tradewind.model;

/** A purchased multiplier with an absolute expiry timestamp. */
public record ActiveBoost(BoostType type, double multiplier, long expiresAt) {

    public boolean active(long now) {
        return expiresAt > now;
    }

    public long remainingMillis(long now) {
        return Math.max(0L, expiresAt - now);
    }
}

