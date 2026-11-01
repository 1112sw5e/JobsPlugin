package com.tradewind.anti;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player rate limit: a minimum interval between actions plus a hard cap per window.
 * Stops click-spam clients and scripted macro farms from multiplying payouts.
 */
public final class ActionLimiter {

    private static final class Window {
        private long last = 0L;
        private long windowStart = 0L;
        private int count;
    }

    private final long minIntervalMillis;
    private final int maxPerWindow;
    private final long windowMillis;
    private final Map<UUID, Window> windows = new ConcurrentHashMap<>();

    public ActionLimiter(long minIntervalMillis, int maxPerSecond) {
        this(minIntervalMillis, maxPerSecond, 1000L);
    }

    public ActionLimiter(long minIntervalMillis, int maxPerWindow, long windowMillis) {
        this.minIntervalMillis = Math.max(0L, minIntervalMillis);
        this.maxPerWindow = Math.max(1, maxPerWindow);
        this.windowMillis = Math.max(100L, windowMillis);
    }

    public boolean allow(UUID uuid) {
        long now = System.currentTimeMillis();
        Window window = windows.computeIfAbsent(uuid, key -> new Window());
        synchronized (window) {
            if (now - window.last < minIntervalMillis) {
                return false;
            }
            if (now - window.windowStart >= windowMillis) {
                window.windowStart = now;
                window.count = 0;
            }
            if (window.count >= maxPerWindow) {
                return false;
            }
            window.count++;
            window.last = now;
            return true;
        }
    }

    public void clear(UUID uuid) {
        windows.remove(uuid);
    }

    public int tracked() {
        return windows.size();
    }
}

