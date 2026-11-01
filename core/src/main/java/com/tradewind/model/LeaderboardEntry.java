package com.tradewind.model;

import java.util.UUID;

/** One row of a profession leaderboard. */
public record LeaderboardEntry(UUID uuid, String name, long actions, double money, long xp, int level) {
}

