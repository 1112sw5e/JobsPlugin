package com.tradewind.model;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Cached, mutable state of one player. Only touched from the main thread. */
public final class PlayerData {

    private final UUID uuid;
    private final Map<Profession, ProfessionStats> stats = new EnumMap<>(Profession.class);
    private final Map<BoostType, ActiveBoost> boosts = new ConcurrentHashMap<>();
    private String name;
    private Profession profession;
    private boolean autoSell;
    private boolean dirty;
    private boolean newProfile;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        for (Profession profession : Profession.values()) {
            stats.put(profession, new ProfessionStats());
        }
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name == null ? "?" : name;
    }

    public void name(String name) {
        if (name != null && !name.equals(this.name)) {
            this.name = name;
            dirty = true;
        }
    }

    public ProfessionStats stats(Profession profession) {
        return stats.get(profession);
    }

    public Optional<Profession> profession() {
        return Optional.ofNullable(profession);
    }

    public void profession(Profession profession) {
        this.profession = profession;
        dirty = true;
    }

    public boolean autoSell() {
        return autoSell;
    }

    public void autoSell(boolean autoSell) {
        this.autoSell = autoSell;
        dirty = true;
    }

    public Collection<ActiveBoost> boosts() {
        return boosts.values();
    }

    public Optional<ActiveBoost> boost(BoostType type) {
        ActiveBoost boost = boosts.get(type);
        if (boost == null || !boost.active(System.currentTimeMillis())) {
            return Optional.empty();
        }
        return Optional.of(boost);
    }

    public void boost(ActiveBoost boost) {
        boosts.put(boost.type(), boost);
        dirty = true;
    }

    public void clearBoost(BoostType type) {
        if (boosts.remove(type) != null) {
            dirty = true;
        }
    }

    /** Drops expired boosts so they are not written back to the database. */
    public void pruneExpiredBoosts(long now) {
        boosts.entrySet().removeIf(entry -> !entry.getValue().active(now));
    }

    public long totalActions() {
        long total = 0L;
        for (ProfessionStats entry : stats.values()) {
            total += entry.actions();
        }
        return total;
    }

    public double totalMoney() {
        double total = 0.0D;
        for (ProfessionStats entry : stats.values()) {
            total += entry.money();
        }
        return total;
    }

    public boolean dirty() {
        return dirty;
    }

    /** True when the profile did not exist in storage yet. */
    public boolean newProfile() {
        return newProfile;
    }

    public void newProfile(boolean newProfile) {
        this.newProfile = newProfile;
    }

    public void dirty(boolean dirty) {
        this.dirty = dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }
}

