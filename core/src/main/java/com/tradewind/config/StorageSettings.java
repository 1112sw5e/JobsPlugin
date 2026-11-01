package com.tradewind.config;

import java.util.Locale;

/** Parsed {@code storage} section. Credentials are kept out of {@code toString()}. */
public record StorageSettings(
        String type,
        String sqliteFile,
        String host,
        int port,
        String database,
        String user,
        String password,
        boolean useSsl,
        int flushIntervalSeconds
) {

    public boolean mysql() {
        return !"sqlite".equals(type);
    }

    public String jdbcUrl(java.nio.file.Path dataFolder) {
        if (mysql()) {
            return "jdbc:mariadb://" + host + ":" + port + "/" + database
                    + "?useSsl=" + useSsl + "&useUnicode=true&characterEncoding=utf8";
        }
        return "jdbc:sqlite:" + dataFolder.resolve(sqliteFile).toAbsolutePath();
    }

    @Override
    public String toString() {
        if (mysql()) {
            return "mysql@" + host + ":" + port + "/" + database + " as " + user;
        }
        return "sqlite:" + sqliteFile;
    }

    public String normalizedType() {
        return type.toLowerCase(Locale.ROOT);
    }
}

