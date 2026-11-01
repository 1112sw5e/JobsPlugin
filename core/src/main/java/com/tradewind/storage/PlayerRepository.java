package com.tradewind.storage;

import com.tradewind.model.LeaderboardEntry;
import com.tradewind.model.Profession;
import com.tradewind.model.PlayerData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.entity.Player;

/**
 * In-memory cache over {@link Storage}: the server thread never waits for SQL.
 * Loads happen on join, dirty profiles are flushed periodically and on quit.
 */
public final class PlayerRepository implements AutoCloseable {

    private final Logger logger;
    private final Storage storage;
    private final DbExecutor db;
    private final boolean defaultAutoSell;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerRepository(Logger logger, Storage storage, DbExecutor db, boolean defaultAutoSell) {
        this.logger = logger;
        this.storage = storage;
        this.db = db;
        this.defaultAutoSell = defaultAutoSell;
    }

    /** Loads a profile off-thread; absent cache entries simply mean "not loaded yet". */
    public void loadAsync(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        db.supply("load " + name, () -> {
            PlayerData data = storage.load(uuid, name);
            boolean renamed = !data.name().equals(name);
            data.name(name);
            if (renamed) {
                data.markDirty();
            }
            return data;
        }).thenAccept(data -> {
            // Only brand-new profiles inherit the configured auto-sell default, so a player who
            // switched auto-sell off is not reset on the next join.
            if (data.newProfile()) {
                data.autoSell(defaultAutoSell);
            }
            cache.put(uuid, data);
        }).exceptionally(failure -> {
            logger.warning("Профиль " + name + " не загружен, статистика временно недоступна.");
            return null;
        });
    }

    public PlayerData get(UUID uuid) {
        return cache.get(uuid);
    }

    public PlayerData get(Player player) {
        return cache.get(player.getUniqueId());
    }

    /** Persists a profile immediately (quit) and drops it from the cache. */
    public void unloadAsync(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data == null) {
            return;
        }
        data.dirty(false);
        PlayerSnapshot snapshot = PlayerSnapshot.of(data);
        db.execute("save " + data.name(), () -> save(snapshot), () -> { });
    }

    /** Flushes every dirty profile; called by the periodic task. */
    public int flushDirty() {
        List<PlayerData> dirty = new ArrayList<>();
        for (PlayerData data : cache.values()) {
            if (data.dirty()) {
                data.dirty(false);
                dirty.add(data);
            }
        }
        for (PlayerData data : dirty) {
            PlayerSnapshot snapshot = PlayerSnapshot.of(data);
            db.execute("flush " + data.name(), () -> save(snapshot), data::markDirty);
        }
        return dirty.size();
    }

    /** Bridges the checked storage exception into the executor's runtime boundary. */
    private void save(PlayerSnapshot snapshot) {
        try {
            storage.save(snapshot);
        } catch (StorageException failure) {
            throw new IllegalStateException(failure.getMessage(), failure);
        }
    }

    public CompletableFuture<List<LeaderboardEntry>> top(Profession profession, int limit) {
        return db.supply("top " + profession.id(), () -> storage.top(profession, limit));
    }

    public String describe() {
        return storage.describe();
    }

    @Override
    public void close() {
        flushDirty();
        db.close();
        storage.close();
    }
}

