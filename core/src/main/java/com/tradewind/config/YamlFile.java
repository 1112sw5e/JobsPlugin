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

    public YamlFile(JavaPlugin plugin, String fileName, int expectedVersion) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.expectedVersion = expectedVersion;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.isFile()) {
            copyDefault(false);
        }
        yaml = YamlConfiguration.loadConfiguration(file);

        int actualVersion = yaml.getInt("version", -1);
        if (actualVersion < 0) {
            plugin.getLogger().warning(fileName + ": отсутствует ключ version, работаю со значениями из файла как есть.");
            applyDefaults(file, actualVersion, false);
            return;
        }
        if (actualVersion > expectedVersion) {
            plugin.getLogger().warning(fileName + " создан более новой версией плагина (v" + actualVersion
                    + ", поддерживается v" + expectedVersion + "). Обновите плагин, чтобы не потерять настройки.");
            return;
        }
        if (actualVersion < expectedVersion) {
            upgrade(file, actualVersion);
        } else {
            applyDefaults(file, actualVersion, false);
        }
    }

    private void upgrade(File file, int actualVersion) {
        Path source = file.toPath();
        Path backup = source.resolveSibling(fileName + ".bak-v" + actualVersion);
        try {
            Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
            applyDefaults(file, expectedVersion, true);
            plugin.getLogger().warning(fileName + ": схема v" + actualVersion + " обновлена до v"
                    + expectedVersion + ". Ваши значения сохранены, недостающие ключи добавлены. "
                    + "Резервная копия: " + backup.getFileName());
        } catch (IOException | RuntimeException failure) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось обновить " + fileName
                    + "; продолжаю с текущим файлом, сделайте резервную копию вручную.", failure);
        }
    }

    /**
     * Adds keys that exist in the bundled file but not in the user's copy, keeping every value
     * the admin already set. This is what lets a new stage introduce settings without wiping
     * an existing configuration.
     */
    private void applyDefaults(File file, int targetVersion, boolean forceSave) {
        try (InputStream stream = plugin.getResource(fileName)) {
            if (stream == null) {
                return;
            }
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            YamlConfiguration current = YamlConfiguration.loadConfiguration(file);
            boolean changed = forceSave;
            boolean added = false;
            for (String key : defaults.getKeys(true)) {
                if (defaults.isConfigurationSection(key) || current.contains(key)) {
                    continue;
                }
                current.set(key, defaults.get(key));
                changed = true;
                added = true;
            }
            if (targetVersion > 0 && current.getInt("version", 0) < targetVersion) {
                current.set("version", targetVersion);
                changed = true;
            }
            if (changed) {
                current.save(file);
                yaml = current;
                if (added) {
                    plugin.getLogger().info(fileName + ": добавлены отсутствующие ключи из версии плагина.");
                }
            }
        } catch (IOException | RuntimeException failure) {
            plugin.getLogger().log(Level.WARNING,
                    "Не удалось дополнить " + fileName + " значениями по умолчанию.", failure);
        }
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

