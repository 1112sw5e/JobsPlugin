package com.tradewind.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * A bundled YAML file with an explicit schema version.
 *
 * <p>Missing file -> copy the jar default. Version older than the plugin's -> back the file up
 * and write fresh defaults (stage 1 has no migrations yet). Version newer than the plugin's ->
 * keep the file untouched and warn, so a downgrade never silently eats settings.
 */
public final class YamlFile {

    private final JavaPlugin plugin;
    private final String fileName;
    private final int expectedVersion;
    private YamlConfiguration yaml = new YamlConfiguration();
    private YamlConfiguration defaults = new YamlConfiguration();

    public YamlFile(JavaPlugin plugin, String fileName, int expectedVersion) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.expectedVersion = expectedVersion;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), fileName);
        loadDefaults();
        if (!file.isFile()) {
            copyDefault(false);
        }
        yaml = YamlConfiguration.loadConfiguration(file);

        int actualVersion = yaml.getInt("version", -1);
        if (actualVersion < 0) {
            plugin.getLogger().warning(fileName + ": отсутствует ключ version, работаю со значениями из файла как есть.");
            return;
        }
        if (actualVersion > expectedVersion) {
            plugin.getLogger().warning(fileName + " создан более новой версией плагина (v" + actualVersion
                    + ", поддерживается v" + expectedVersion + "). Обновите плагин, чтобы не потерять настройки.");
            return;
        }
        if (actualVersion < expectedVersion) {
            upgrade(file, actualVersion);
        }
    }

    private void upgrade(File file, int actualVersion) {
        Path source = file.toPath();
        Path backup = source.resolveSibling(fileName + ".bak-v" + actualVersion);
        try {
            Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
            copyDefault(true);
            yaml = YamlConfiguration.loadConfiguration(file);
            plugin.getLogger().warning(fileName + ": схема v" + actualVersion + " заменена на v"
                    + expectedVersion + " с безопасными значениями. Резервная копия с вашими правками: "
                    + backup.getFileName());
        } catch (IOException | RuntimeException failure) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось обновить " + fileName
                    + "; продолжаю с текущим файлом, сделайте резервную копию вручную.", failure);
        }
    }

    /**
     * Reads the bundled file once per reload so callers can fall back to a default when an
     * admin's copy predates a newly introduced key. The user's file is never rewritten here:
     * Bukkit's YAML writer would drop every comment.
     */
    private void loadDefaults() {
        try (InputStream stream = plugin.getResource(fileName)) {
            if (stream == null) {
                defaults = new YamlConfiguration();
                return;
            }
            defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException failure) {
            defaults = new YamlConfiguration();
            plugin.getLogger().log(Level.WARNING,
                    "Не удалось прочитать значения по умолчанию для " + fileName + ".", failure);
        }
    }

    /** Bundled values, used when the admin's file does not contain a key yet. */
    public YamlConfiguration bundledDefaults() {
        return defaults;
    }

    private void copyDefault(boolean overwrite) {
        try {
            plugin.saveResource(fileName, overwrite);
        } catch (RuntimeException failure) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось записать " + fileName + " из ресурсов плагина.", failure);
        }
    }

    public YamlConfiguration yaml() {
        return yaml;
    }

    public int version() {
        return yaml.getInt("version", -1);
    }

    public String fileName() {
        return fileName;
    }
}

