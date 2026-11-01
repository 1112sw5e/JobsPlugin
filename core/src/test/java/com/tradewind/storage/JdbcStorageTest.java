package com.tradewind.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tradewind.config.StorageSettings;
import com.tradewind.model.ActiveBoost;
import com.tradewind.model.BoostType;
import com.tradewind.model.Profession;
import com.tradewind.model.PlayerData;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JdbcStorageTest {

    @TempDir
    Path folder;

    private JdbcStorage storage;

    @BeforeEach
    void setUp() throws Exception {
        StorageSettings settings = new StorageSettings("sqlite", "test.db", "127.0.0.1", 3306,
                "tradewind", "user", "", false, 30);
        storage = new JdbcStorage(settings, folder, Logger.getLogger("test"));
        storage.init();
    }

    @AfterEach
    void tearDown() {
        storage.close();
    }

    @Test
    void roundTripsProfile() throws Exception {
        UUID uuid = UUID.randomUUID();
        PlayerData data = new PlayerData(uuid);
        data.name("Steve");
        data.profession(Profession.MINE);
        data.autoSell(true);
        data.stats(Profession.MINE).restore(42L, 123.45D, 900L, 4);
        data.stats(Profession.FARM).restore(3L, 0.5D, 10L, 1);
        data.boost(new ActiveBoost(BoostType.MONEY, 1.5D, System.currentTimeMillis() + 60_000L));

        storage.save(PlayerSnapshot.of(data));
        PlayerData loaded = storage.load(uuid, "Steve");

        assertEquals("Steve", loaded.name());
        assertEquals(Profession.MINE, loaded.profession().orElseThrow());
        assertTrue(loaded.autoSell());
        assertEquals(42L, loaded.stats(Profession.MINE).actions());
        assertEquals(123.45D, loaded.stats(Profession.MINE).money(), 0.0001);
        assertEquals(900L, loaded.stats(Profession.MINE).xp());
        assertEquals(4, loaded.stats(Profession.MINE).level());
        assertEquals(3L, loaded.stats(Profession.FARM).actions());
        assertEquals(1.5D, loaded.boost(BoostType.MONEY).orElseThrow().multiplier(), 0.0001);
    }

    @Test
    void newPlayerGetsEmptyProfile() throws Exception {
        UUID uuid = UUID.randomUUID();
        PlayerData loaded = storage.load(uuid, "Alex");
        assertEquals("Alex", loaded.name());
        assertFalse(loaded.profession().isPresent());
        assertEquals(0L, loaded.stats(Profession.WOOD).actions());
    }

    @Test
    void savingTwiceUpdatesInsteadOfDuplicating() throws Exception {
        UUID uuid = UUID.randomUUID();
        PlayerData data = new PlayerData(uuid);
        data.name("Steve");
        data.stats(Profession.WOOD).restore(5L, 1.0D, 5L, 1);
        storage.save(PlayerSnapshot.of(data));

        data.stats(Profession.WOOD).restore(9L, 2.0D, 9L, 2);
        storage.save(PlayerSnapshot.of(data));

        PlayerData loaded = storage.load(uuid, "Steve");
        assertEquals(9L, loaded.stats(Profession.WOOD).actions());
        assertEquals(2, loaded.stats(Profession.WOOD).level());
    }

    @Test
    void leaderboardOrdersByActions() throws Exception {
        for (int index = 0; index < 3; index++) {
            PlayerData data = new PlayerData(UUID.randomUUID());
            data.name("player" + index);
            data.stats(Profession.SNOW).restore(index * 10L, 0.0D, 0L, 1);
            storage.save(PlayerSnapshot.of(data));
        }
        var top = storage.top(Profession.SNOW, 2);
        assertEquals(2, top.size());
        assertEquals(20L, top.get(0).actions());
        assertEquals("player2", top.get(0).name());
    }
}

