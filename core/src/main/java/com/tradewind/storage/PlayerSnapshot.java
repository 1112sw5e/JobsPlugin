package com.tradewind.storage;

import com.tradewind.model.ActiveBoost;
import com.tradewind.model.Profession;
import com.tradewind.model.ProfessionStats;
import com.tradewind.model.PlayerData;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable copy of {@link PlayerData}, handed to the database thread.
 * The live object stays owned by the main thread.
 */
public record PlayerSnapshot(
        UUID uuid,
        String name,
        String profession,
        boolean autoSell,
        Map<Profession, StatsSnapshot> stats,
        List<ActiveBoost> boosts
) {

    public record StatsSnapshot(long actions, double money, long xp, int level) {
    }

    public static PlayerSnapshot of(PlayerData data) {
        Map<Profession, StatsSnapshot> stats = new EnumMap<>(Profession.class);
        for (Profession profession : Profession.values()) {
            ProfessionStats source = data.stats(profession);
            stats.put(profession, new StatsSnapshot(source.actions(), source.money(),
                    source.xp(), source.level()));
        }
        List<ActiveBoost> boosts = new ArrayList<>(data.boosts());
        return new PlayerSnapshot(
                data.uuid(),
                data.name(),
                data.profession().map(Profession::id).orElse(null),
                data.autoSell(),
                stats,
                List.copyOf(boosts)
        );
    }
}

