package com.tradewind.util;

import java.util.Locale;

/** Duration parsing ("30m", "2h", "90s") and human formatting. */
public final class TimeUtil {

    private TimeUtil() {
    }

    public static long parseMillis(String input, long fallback) {
        if (input == null || input.isBlank()) {
            return fallback;
        }
        String trimmed = input.trim().toLowerCase(Locale.ROOT);
        long multiplier = 1000L;
        char unit = trimmed.charAt(trimmed.length() - 1);
        if (Character.isLetter(unit)) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
            multiplier = switch (unit) {
                case 's' -> 1000L;
                case 'm' -> 60_000L;
                case 'h' -> 3_600_000L;
                case 'd' -> 86_400_000L;
                default -> -1L;
            };
            if (multiplier < 0) {
                return fallback;
            }
        }
        try {
            return Long.parseLong(trimmed.trim()) * multiplier;
        } catch (NumberFormatException invalid) {
            return fallback;
        }
    }

    /** 3_725_000 -> "1ч 2м 5с". */
    public static String format(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long remaining = seconds % 60L;
        if (hours > 0) {
            return hours + "ч " + minutes + "м";
        }
        if (minutes > 0) {
            return minutes + "м " + remaining + "с";
        }
        return remaining + "с";
    }

    public static String formatSeconds(long seconds) {
        return format(seconds * 1000L);
    }
}

