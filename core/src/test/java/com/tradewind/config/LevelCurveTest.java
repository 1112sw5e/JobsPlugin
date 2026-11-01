package com.tradewind.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class LevelCurveTest {

    private LevelCurve curve() {
        // base 100, exponent 1.0 -> 100 xp per level
        return new LevelCurve(true, 10, 100.0D, 1.0D, 0.02D, Map.of(5, 500.0D));
    }

    @Test
    void levelFollowsCumulativeThresholds() {
        LevelCurve curve = curve();
        assertEquals(1, curve.levelForXp(0));
        assertEquals(1, curve.levelForXp(99));
        assertEquals(2, curve.levelForXp(100));
        assertEquals(2, curve.levelForXp(250));
        assertEquals(3, curve.levelForXp(300));
        assertEquals(10, curve.levelForXp(100_000));
    }

    @Test
    void progressStaysInsideTheLevel() {
        LevelCurve curve = curve();
        assertEquals(0, curve.progressPercent(100, 2));
        assertEquals(25, curve.progressPercent(150, 2));
        assertEquals(100, curve.progressPercent(999_999, 10));
    }

    @Test
    void salaryGrowsWithLevel() {
        LevelCurve curve = curve();
        assertEquals(1.0D, curve.salaryMultiplier(1), 0.0001);
        assertEquals(1.04D, curve.salaryMultiplier(3), 0.0001);
    }

    @Test
    void rewardsAreOptional() {
        assertEquals(500.0D, curve().reward(5), 0.0001);
        assertEquals(0.0D, curve().reward(6), 0.0001);
    }

    @Test
    void disabledCurveNeverLevels() {
        LevelCurve disabled = new LevelCurve(false, 100, 100.0D, 1.5D, 0.02D, Map.of());
        assertFalse(disabled.enabled());
        assertEquals(1, disabled.levelForXp(1_000_000));
        assertEquals(0L, disabled.xpToNextLevel(1));
    }

    @Test
    void enabledCurveReportsNextRequirement() {
        LevelCurve curve = curve();
        assertTrue(curve.enabled());
        assertEquals(100L, curve.xpToNextLevel(1));
        assertEquals(0L, curve.xpToNextLevel(10));
    }
}

