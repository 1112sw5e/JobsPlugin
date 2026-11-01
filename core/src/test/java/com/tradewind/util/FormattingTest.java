package com.tradewind.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FormattingTest {

    @Test
    void roundsHalfUp() {
        assertEquals(2.35D, Numbers.round(2.345D, 2), 0.00001);
        assertEquals(3.0D, Numbers.round(2.5D, 0), 0.00001);
    }

    @Test
    void compactsLargeAmounts() {
        assertEquals("999", Numbers.compact(999));
        assertEquals("1.23K", Numbers.compact(1234));
        assertEquals("1.23M", Numbers.compact(1_234_567));
    }

    @Test
    void parsesDurations() {
        assertEquals(1_800_000L, TimeUtil.parseMillis("30m", 0L));
        assertEquals(90_000L, TimeUtil.parseMillis("90s", 0L));
        assertEquals(7_200_000L, TimeUtil.parseMillis("2h", 0L));
        assertEquals(5_000L, TimeUtil.parseMillis("nonsense", 5_000L));
    }

    @Test
    void formatsDurations() {
        assertEquals("1ч 2м", TimeUtil.format(3_725_000L));
        assertEquals("2м 5с", TimeUtil.format(125_000L));
        assertEquals("9с", TimeUtil.format(9_000L));
    }

    @Test
    void parsesPositiveNumbersSafely() {
        assertEquals(7, Numbers.parsePositive("7", 1));
        assertEquals(1, Numbers.parsePositive("-4", 1));
        assertEquals(1, Numbers.parsePositive("abc", 1));
    }
}

