package com.tradewind.service;

import com.tradewind.bootstrap.PluginContext;
import com.tradewind.model.LeaderboardEntry;
import com.tradewind.model.Profession;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Cached per-profession leaderboards, refreshed off-thread. */
public final class LeaderboardService {

    private final PluginContext context;
    private final Map<Profession, List<LeaderboardEntry>> cache = new EnumMap<>(Profession.class);

    public LeaderboardService(PluginContext context) {
        this.context = context;
    }

    public void refreshAsync() {
        for (Profession profession : Profession.values()) {
            context.players().top(profession, 10).thenAccept(entries -> {
                synchronized (cache) {
                    cache.put(profession, List.copyOf(entries));
                }
            }).exceptionally(failure -> {
                context.plugin().getLogger().warning("Не удалось обновить топ "
                        + profession.id() + ": " + failure.getMessage());
                return null;
            });
        }
    }

    public List<LeaderboardEntry> top(Profession profession) {
        synchronized (cache) {
            return cache.getOrDefault(profession, List.of());
        }
    }

    public LeaderboardEntry at(Profession profession, int rank) {
        List<LeaderboardEntry> entries = top(profession);
        return rank >= 1 && rank <= entries.size() ? entries.get(rank - 1) : null;
    }
}

