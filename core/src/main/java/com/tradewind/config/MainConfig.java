package com.tradewind.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Typed, validated view of {@code config.yml}. Every accessor has a safe fallback,
 * so a hand-edited or truncated file never throws during startup.
 */
public final class MainConfig {

    public static final int VERSION = 1;

    private static final Set<String> KNOWN_PROFESSIONS = Set.of("mine", "farm", "wood", "fish", "snow");
    private static final Set<String> STORAGE_TYPES = Set.of("sqlite", "mysql", "mariadb");

    private final YamlFile file;
    private final List<String> warnings = new ArrayList<>();
    private final Set<String> enabledProfessions = new LinkedHashSet<>();

    private boolean debug;
    private boolean failFast;
    private boolean warnOnMissingOptional;
    private String currencyName;
    private int economyRoundTo;
    private String storageType;
    private boolean buyerEnabled;
    private boolean levelsEnabled;
    private boolean boostsEnabled;

    public MainConfig(JavaPlugin plugin) {
        this.file = new YamlFile(plugin, "config.yml", VERSION);
    }

    public void reload() {
        file.reload();
        warnings.clear();
        enabledProfessions.clear();

        var yaml = file.yaml();
        debug = yaml.getBoolean("general.debug", false);
        failFast = yaml.getBoolean("dependencies.fail-fast", true);
        warnOnMissingOptional = yaml.getBoolean("dependencies.warn-on-missing-optional", true);
        currencyName = yaml.getString("economy.currency-name", "монеты");
        economyRoundTo = clamp(yaml.getInt("economy.round-to", 2), 0, 4);
        storageType = yaml.getString("storage.type", "sqlite").toLowerCase(Locale.ROOT);
        buyerEnabled = yaml.getBoolean("buyer.enabled", false);
        levelsEnabled = yaml.getBoolean("levels.enabled", false);
        boostsEnabled = yaml.getBoolean("boosts.enabled", false);

        readProfessions(yaml.getConfigurationSection("professions"));
        validate();
    }

    private void readProfessions(ConfigurationSection section) {
        if (section == null) {
            warnings.add("config.yml: секция professions отсутствует, профессии считаются выключенными.");
            return;
        }
        for (String key : section.getKeys(false)) {
            if (!KNOWN_PROFESSIONS.contains(key)) {
                warnings.add("config.yml: неизвестная профессия '" + key + "' — доступны " + KNOWN_PROFESSIONS + ".");
                continue;
            }
            if (section.getBoolean(key + ".enabled", false)) {
                enabledProfessions.add(key);
            }
        }
        if (enabledProfessions.isEmpty()) {
            warnings.add("config.yml: ни одна профессия не включена — механика этапа 2 будет неактивна.");
        }
    }

    private void validate() {
        if (!STORAGE_TYPES.contains(storageType)) {
            warnings.add("config.yml: storage.type='" + storageType + "' неизвестен, использую sqlite ('"
                    + String.join("', '", STORAGE_TYPES) + "').");
            storageType = "sqlite";
        }
        if (currencyName == null || currencyName.isBlank()) {
            warnings.add("config.yml: economy.currency-name пуст, использую 'монеты'.");
            currencyName = "монеты";
        }
        if (buyerEnabled) {
            warnings.add("config.yml: buyer.enabled=true, но скупщик появится на этапе 4 — настройка пока не действует.");
        }
        if (levelsEnabled) {
            warnings.add("config.yml: levels.enabled=true, уровни появятся на этапе 5 — настройка пока не действует.");
        }
        if (boostsEnabled) {
            warnings.add("config.yml: boosts.enabled=true, бусты появятся на этапе 6 — настройка пока не действует.");
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public int version() {
        return file.version();
    }

    public List<String> warnings() {
        return Collections.unmodifiableList(warnings);
    }

    public boolean debug() {
        return debug;
    }

    public boolean failFast() {
        return failFast;
    }

    public boolean warnOnMissingOptional() {
        return warnOnMissingOptional;
    }

    public String currencyName() {
        return currencyName;
    }

    public int economyRoundTo() {
        return economyRoundTo;
    }

    public String storageType() {
        return storageType;
    }

    public Set<String> enabledProfessions() {
        return Collections.unmodifiableSet(enabledProfessions);
    }

    public boolean buyerEnabled() {
        return buyerEnabled;
    }
}

