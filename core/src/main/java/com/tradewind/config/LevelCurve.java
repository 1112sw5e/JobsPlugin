package com.tradewind.config;

import java.util.Collections;
import java.util.Map;

/** Level thresholds and salary multipliers derived from a simple power curve. */
public final class LevelCurve {

    private final boolean enabled;
    private final int maxLevel;
    private final double salaryPerLevel;
    private final Map<Integer, Double> rewards;
    private final long[] threshold;

    public LevelCurve(boolean enabled, int maxLevel, double baseXp, double exponent,
                      double salaryPerLevel, Map<Integer, Double> rewards) {
        this.enabled = enabled && maxLevel > 1;
        this.maxLevel = Math.max(1, maxLevel);
        this.salaryPerLevel = Math.max(0.0D, salaryPerLevel);
        this.rewards = Collections.unmodifiableMap(rewards);
        this.threshold = new long[this.maxLevel + 1];
        // threshold[level] = total xp needed to reach that level
        for (int level = 1; level < this.maxLevel; level++) {
            long step = Math.round(Math.max(1.0D, baseXp * Math.pow(level, Math.max(0.1D, exponent))));
            threshold[level + 1] = threshold[level] + step;
        }
    }

    public boolean enabled() {
        return enabled;
    }

    public int maxLevel() {
        return maxLevel;
    }

    public int levelForXp(long xp) {
        if (!enabled) {
            return 1;
        }
        int level = 1;
        while (level < maxLevel && xp >= threshold[level + 1]) {
            level++;
        }
        return level;
    }

    /** Xp progress inside the current level. */
    public long xpIntoLevel(long xp, int level) {
        if (!enabled || level >= maxLevel) {
            return 0L;
        }
        return Math.max(0L, xp - threshold[Math.min(level, maxLevel)]);
    }

    /** Xp needed to reach the next level, 0 at max level. */
    public long xpToNextLevel(int level) {
        if (!enabled || level >= maxLevel) {
            return 0L;
        }
        return threshold[level + 1] - threshold[level];
    }

    /** 0..100 progress inside the current level. */
    public int progressPercent(long xp, int level) {
        long needed = xpToNextLevel(level);
        if (needed <= 0L) {
            return 100;
        }
        return (int) Math.min(100L, xpIntoLevel(xp, level) * 100L / needed);
    }

    public double salaryMultiplier(int level) {
        return 1.0D + Math.max(0, level - 1) * salaryPerLevel;
    }

    /** One-off reward for reaching {@code level}, 0 when none is configured. */
    public double reward(int level) {
        return rewards.getOrDefault(level, 0.0D);
    }
}

