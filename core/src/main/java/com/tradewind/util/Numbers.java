package com.tradewind.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Number formatting shared by GUIs, messages and placeholders. */
public final class Numbers {

    private static final String[] SUFFIXES = {"", "K", "M", "B", "T"};

    private Numbers() {
    }

    public static double round(double value, int decimals) {
        return BigDecimal.valueOf(value)
                .setScale(Math.max(0, decimals), RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static String fixed(double value, int decimals) {
        return String.format(Locale.ROOT, "%." + Math.max(0, Math.min(6, decimals)) + "f", value);
    }

    /** 1234567 -> "1.23M"; used where a fixed column width matters. */
    public static String compact(double value) {
        double absolute = Math.abs(value);
        int index = 0;
        while (absolute >= 1000.0D && index < SUFFIXES.length - 1) {
            absolute /= 1000.0D;
            value /= 1000.0D;
            index++;
        }
        return index == 0 ? fixed(value, 0) : fixed(value, 2) + SUFFIXES[index];
    }

    public static long parseLong(String input, long fallback) {
        try {
            return Long.parseLong(input.trim());
        } catch (NumberFormatException invalid) {
            return fallback;
        }
    }

    public static int parsePositive(String input, int fallback) {
        try {
            int value = Integer.parseInt(input.trim());
            return value > 0 ? value : fallback;
        } catch (NumberFormatException invalid) {
            return fallback;
        }
    }
}

