package com.tradewind.storage;

import com.tradewind.model.LeaderboardEntry;
import com.tradewind.model.Profession;
import com.tradewind.model.PlayerData;
import java.util.List;
import java.util.UUID;

/** Persistence contract. Every method is called from the database thread. */
public interface Storage extends AutoCloseable {

    void init() throws StorageException;

    /** Returns stored data or an empty profile when the player is new. */
    PlayerData load(UUID uuid, String fallbackName) throws StorageException;

    void save(PlayerSnapshot snapshot) throws StorageException;

    List<LeaderboardEntry> top(Profession profession, int limit) throws StorageException;

    String describe();

    @Override
    void close();
}

