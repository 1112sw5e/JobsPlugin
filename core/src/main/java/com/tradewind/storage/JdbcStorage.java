package com.tradewind.storage;

import com.tradewind.config.StorageSettings;
import com.tradewind.model.ActiveBoost;
import com.tradewind.model.BoostType;
import com.tradewind.model.LeaderboardEntry;
import com.tradewind.model.Profession;
import com.tradewind.model.PlayerData;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * JDBC storage for SQLite and MySQL/MariaDB.
 *
 * <p>All access happens on the database thread, so one connection is enough and no pool is
 * needed. The MariaDB driver speaks the MySQL protocol, which keeps the jar small.
 */
public final class JdbcStorage implements Storage {

    private final StorageSettings settings;
    private final Path dataFolder;
    private final Logger logger;
    private Connection connection;

    public JdbcStorage(StorageSettings settings, Path dataFolder, Logger logger) {
        this.settings = settings;
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    @Override
    public void init() throws StorageException {
        try {
            loadDriver();
            connection = DriverManager.getConnection(settings.jdbcUrl(dataFolder),
                    settings.mysql() ? settings.user() : null,
                    settings.mysql() ? settings.password() : null);
            connection.setAutoCommit(true);
            createTables();
            logger.info("Хранилище готово: " + settings);
        } catch (SQLException | ClassNotFoundException failure) {
            throw new StorageException("Не удалось открыть хранилище (" + settings + "): "
                    + failure.getMessage(), failure);
        }
    }

    private void loadDriver() throws ClassNotFoundException {
        if (settings.mysql()) {
            Class.forName("org.mariadb.jdbc.Driver");
        } else {
            Class.forName("org.sqlite.JDBC");
        }
    }

    private void createTables() throws SQLException {
        String players = settings.mysql()
                ? "CREATE TABLE IF NOT EXISTS tw_players (uuid VARCHAR(36) PRIMARY KEY, "
                + "name VARCHAR(32) NOT NULL, profession VARCHAR(16) NULL, "
                + "autosell TINYINT NOT NULL DEFAULT 0, updated_at BIGINT NOT NULL) "
                + "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                : "CREATE TABLE IF NOT EXISTS tw_players (uuid TEXT PRIMARY KEY, "
                + "name TEXT NOT NULL, profession TEXT NULL, "
                + "autosell INTEGER NOT NULL DEFAULT 0, updated_at INTEGER NOT NULL)";
        String stats = settings.mysql()
                ? "CREATE TABLE IF NOT EXISTS tw_stats (uuid VARCHAR(36) NOT NULL, "
                + "profession VARCHAR(16) NOT NULL, actions BIGINT NOT NULL DEFAULT 0, "
                + "money DOUBLE NOT NULL DEFAULT 0, xp BIGINT NOT NULL DEFAULT 0, "
                + "level INT NOT NULL DEFAULT 1, PRIMARY KEY (uuid, profession)) "
                + "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                : "CREATE TABLE IF NOT EXISTS tw_stats (uuid TEXT NOT NULL, "
                + "profession TEXT NOT NULL, actions INTEGER NOT NULL DEFAULT 0, "
                + "money REAL NOT NULL DEFAULT 0, xp INTEGER NOT NULL DEFAULT 0, "
                + "level INTEGER NOT NULL DEFAULT 1, PRIMARY KEY (uuid, profession))";
        String boosts = settings.mysql()
                ? "CREATE TABLE IF NOT EXISTS tw_boosts (uuid VARCHAR(36) NOT NULL, "
                + "type VARCHAR(16) NOT NULL, multiplier DOUBLE NOT NULL, expires BIGINT NOT NULL, "
                + "PRIMARY KEY (uuid, type)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                : "CREATE TABLE IF NOT EXISTS tw_boosts (uuid TEXT NOT NULL, "
                + "type TEXT NOT NULL, multiplier REAL NOT NULL, expires INTEGER NOT NULL, "
                + "PRIMARY KEY (uuid, type))";
        try (Statement statement = connection.createStatement()) {
            statement.execute(players);
            statement.execute(stats);
            statement.execute(boosts);
        }
    }

    private Connection connection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            logger.warning("Соединение с БД потеряно, переподключаюсь.");
            connection = DriverManager.getConnection(settings.jdbcUrl(dataFolder),
                    settings.mysql() ? settings.user() : null,
                    settings.mysql() ? settings.password() : null);
        }
        return connection;
    }

    @Override
    public PlayerData load(UUID uuid, String fallbackName) throws StorageException {
        try {
            PlayerData data = new PlayerData(uuid);
            try (PreparedStatement select = connection().prepareStatement(
                    "SELECT name, profession, autosell FROM tw_players WHERE uuid = ?")) {
                select.setString(1, uuid.toString());
                try (ResultSet row = select.executeQuery()) {
                    if (row.next()) {
                        data.name(row.getString(1));
                        String profession = row.getString(2);
                        if (profession != null) {
                            Profession.byId(profession).ifPresent(data::profession);
                        }
                        data.autoSell(row.getInt(3) != 0);
                    } else {
                        data.name(fallbackName);
                        data.newProfile(true);
                    }
                }
            }
            try (PreparedStatement select = connection().prepareStatement(
                    "SELECT profession, actions, money, xp, level FROM tw_stats WHERE uuid = ?")) {
                select.setString(1, uuid.toString());
                try (ResultSet row = select.executeQuery()) {
                    while (row.next()) {
                        var profession = Profession.byId(row.getString(1));
                        if (profession.isEmpty()) {
                            continue;
                        }
                        long actions = row.getLong(2);
                        double money = row.getDouble(3);
                        long xp = row.getLong(4);
                        int level = row.getInt(5);
                        data.stats(profession.get()).restore(actions, money, xp, level);
                    }
                }
            }
            try (PreparedStatement select = connection().prepareStatement(
                    "SELECT type, multiplier, expires FROM tw_boosts WHERE uuid = ?")) {
                select.setString(1, uuid.toString());
                try (ResultSet row = select.executeQuery()) {
                    while (row.next()) {
                        BoostType type = BoostType.byId(row.getString(1));
                        if (type != null) {
                            data.boost(new ActiveBoost(type, row.getDouble(2), row.getLong(3)));
                        }
                    }
                }
            }
            data.dirty(false);
            return data;
        } catch (SQLException failure) {
            throw new StorageException("Не удалось загрузить профиль " + uuid + ": "
                    + failure.getMessage(), failure);
        }
    }

    @Override
    public void save(PlayerSnapshot snapshot) throws StorageException {
        try {
            Connection connection = connection();
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                upsertPlayer(connection, snapshot);
                for (var entry : snapshot.stats().entrySet()) {
                    upsertStats(connection, snapshot.uuid(), entry.getKey(), entry.getValue());
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM tw_boosts WHERE uuid = ?")) {
                    delete.setString(1, snapshot.uuid().toString());
                    delete.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO tw_boosts (uuid, type, multiplier, expires) VALUES (?, ?, ?, ?)")) {
                    for (ActiveBoost boost : snapshot.boosts()) {
                        insert.setString(1, snapshot.uuid().toString());
                        insert.setString(2, boost.type().id());
                        insert.setDouble(3, boost.multiplier());
                        insert.setLong(4, boost.expiresAt());
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
                connection.commit();
            } catch (SQLException failure) {
                connection.rollback();
                throw failure;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException failure) {
            throw new StorageException("Не удалось сохранить профиль " + snapshot.uuid() + ": "
                    + failure.getMessage(), failure);
        }
    }

    private void upsertPlayer(Connection connection, PlayerSnapshot snapshot) throws SQLException {
        String sql = settings.mysql()
                ? "INSERT INTO tw_players (uuid, name, profession, autosell, updated_at) "
                + "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE name = VALUES(name), "
                + "profession = VALUES(profession), autosell = VALUES(autosell), "
                + "updated_at = VALUES(updated_at)"
                : "INSERT INTO tw_players (uuid, name, profession, autosell, updated_at) "
                + "VALUES (?, ?, ?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, "
                + "profession = excluded.profession, autosell = excluded.autosell, "
                + "updated_at = excluded.updated_at";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, snapshot.uuid().toString());
            statement.setString(2, snapshot.name());
            statement.setString(3, snapshot.profession());
            statement.setInt(4, snapshot.autoSell() ? 1 : 0);
            statement.setLong(5, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private void upsertStats(Connection connection, UUID uuid, Profession profession,
                             PlayerSnapshot.StatsSnapshot stats) throws SQLException {
        String sql = settings.mysql()
                ? "INSERT INTO tw_stats (uuid, profession, actions, money, xp, level) "
                + "VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE actions = VALUES(actions), "
                + "money = VALUES(money), xp = VALUES(xp), level = VALUES(level)"
                : "INSERT INTO tw_stats (uuid, profession, actions, money, xp, level) "
                + "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT(uuid, profession) DO UPDATE SET "
                + "actions = excluded.actions, money = excluded.money, xp = excluded.xp, "
                + "level = excluded.level";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, profession.id());
            statement.setLong(3, stats.actions());
            statement.setDouble(4, stats.money());
            statement.setLong(5, stats.xp());
            statement.setInt(6, stats.level());
            statement.executeUpdate();
        }
    }

    @Override
    public List<LeaderboardEntry> top(Profession profession, int limit) throws StorageException {
        List<LeaderboardEntry> entries = new ArrayList<>(limit);
        String sql = "SELECT s.uuid, p.name, s.actions, s.money, s.xp, s.level "
                + "FROM tw_stats s LEFT JOIN tw_players p ON p.uuid = s.uuid "
                + "WHERE s.profession = ? ORDER BY s.actions DESC LIMIT ?";
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setString(1, profession.id());
            statement.setInt(2, limit);
            try (ResultSet row = statement.executeQuery()) {
                while (row.next()) {
                    UUID uuid = UUID.fromString(row.getString(1));
                    String name = row.getString(2);
                    entries.add(new LeaderboardEntry(uuid, name == null ? "?" : name,
                            row.getLong(3), row.getDouble(4), row.getLong(5), row.getInt(6)));
                }
            }
            return entries;
        } catch (SQLException failure) {
            throw new StorageException("Не удалось прочитать топ: " + failure.getMessage(), failure);
        }
    }

    @Override
    public String describe() {
        return settings.toString();
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException failure) {
            logger.warning("Не удалось закрыть соединение с БД: " + failure.getMessage());
        }
    }
}

