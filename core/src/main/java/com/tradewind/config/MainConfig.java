package com.tradewind.config;

import com.tradewind.model.BoostType;
import com.tradewind.model.Profession;
import com.tradewind.util.Numbers;
import com.tradewind.util.TimeUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Typed, validated view of {@code config.yml}, {@code prices.yml}, {@code boosts.yml}
 * and {@code shop.yml}. Every accessor falls back to a safe value, so a hand-edited
 * or truncated file never breaks startup.
 */
public final class MainConfig {

    public static final int VERSION = 2;
    public static final int PRICES_VERSION = 1;
    public static final int BOOSTS_VERSION = 1;
    public static final int SHOP_VERSION = 1;

    private static final Set<String> STORAGE_TYPES = Set.of("sqlite", "mysql", "mariadb");

    private static final Map<String, String> LEGACY_ENCHANTMENTS = Map.ofEntries(
            Map.entry("durability", "unbreaking"),
            Map.entry("damage_all", "sharpness"),
            Map.entry("damage_undead", "smite"),
            Map.entry("damage_arthropods", "bane_of_arthropods"),
            Map.entry("dig_speed", "efficiency"),
            Map.entry("loot_bonus_blocks", "fortune"),
            Map.entry("loot_bonus_mobs", "looting"),
            Map.entry("protection_environmental", "protection"),
            Map.entry("protection_explosives", "blast_protection"),
            Map.entry("protection_fall", "feather_falling"),
            Map.entry("protection_fire", "fire_protection"),
            Map.entry("protection_projectile", "projectile_protection"),
            Map.entry("arrow_damage", "power"),
            Map.entry("arrow_knockback", "punch"),
            Map.entry("arrow_fire", "flame"),
            Map.entry("arrow_infinite", "infinity"),
            Map.entry("sweep_all", "sweeping_edge"),
            Map.entry("water_worker", "aqua_affinity"),
            Map.entry("oxygen", "respiration")
    );

    private final JavaPlugin plugin;
    private final YamlFile configFile;
    private final YamlFile pricesFile;
    private final YamlFile boostsFile;
    private final YamlFile shopFile;
    private final List<String> warnings = new ArrayList<>();

    private boolean debug;
    private boolean failFast;
    private boolean warnOnMissingOptional;
    private String currencyName;
    private int economyRoundTo;
    private StorageSettings storage;
    private final Map<Profession, ProfessionSettings> professions = new EnumMap<>(Profession.class);
    private final Map<String, Profession> regionIndex = new HashMap<>();
    private AntiExploitSettings antiExploit;
    private LevelCurve levels;
    private BuyerSettings buyer;
    private PriceIndex prices;
    private final Map<BoostType, BoostDefinition> boosts = new EnumMap<>(BoostType.class);
    private ShopCatalog shop;
    private boolean allowProfessionChange;
    private long professionChangeCooldownMillis;

    public MainConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new YamlFile(plugin, "config.yml", VERSION);
        this.pricesFile = new YamlFile(plugin, "prices.yml", PRICES_VERSION);
        this.boostsFile = new YamlFile(plugin, "boosts.yml", BOOSTS_VERSION);
        this.shopFile = new YamlFile(plugin, "shop.yml", SHOP_VERSION);
    }

    public void reload() {
        configFile.reload();
        pricesFile.reload();
        boostsFile.reload();
        shopFile.reload();

        warnings.clear();
        professions.clear();
        regionIndex.clear();
        boosts.clear();

        readGeneral();
        readStorage();
        readProfessions();
        readAntiExploit();
        readLevels();
        readBuyer();
        readBoosts();
        readPrices();
        readShop();
        validate();
    }

    // ---------------------------------------------------------------- general

    private void readGeneral() {
        var yaml = configFile.yaml();
        debug = yaml.getBoolean("general.debug", false);
        failFast = yaml.getBoolean("dependencies.fail-fast", true);
        warnOnMissingOptional = yaml.getBoolean("dependencies.warn-on-missing-optional", true);
        currencyName = yaml.getString("economy.currency-name", "монеты");
        economyRoundTo = clamp(yaml.getInt("economy.round-to", 2), 0, 4);
        allowProfessionChange = yaml.getBoolean("professions.allow-change", true);
        professionChangeCooldownMillis = TimeUtil.parseMillis(
                yaml.getString("professions.change-cooldown", "1h"), 3_600_000L);
    }

    private void readStorage() {
        var yaml = configFile.yaml();
        String type = yaml.getString("storage.type", "sqlite").toLowerCase(Locale.ROOT);
        if (!STORAGE_TYPES.contains(type)) {
            warnings.add("config.yml: storage.type='" + type + "' неизвестен, использую sqlite.");
            type = "sqlite";
        }
        storage = new StorageSettings(
                type,
                yaml.getString("storage.sqlite.file", "tradewind.db"),
                yaml.getString("storage.mysql.host", "127.0.0.1"),
                clamp(yaml.getInt("storage.mysql.port", 3306), 1, 65_535),
                yaml.getString("storage.mysql.database", "tradewind"),
                yaml.getString("storage.mysql.user", "tradewind"),
                yaml.getString("storage.mysql.password", ""),
                yaml.getBoolean("storage.mysql.use-ssl", true),
                clamp(yaml.getInt("storage.flush-interval-seconds", 30), 5, 600)
        );
    }

    private void readProfessions() {
        ConfigurationSection section = configFile.yaml().getConfigurationSection("professions");
        for (Profession profession : Profession.values()) {
            ConfigurationSection own = section == null ? null : section.getConfigurationSection(profession.id());
            boolean enabled = own != null && own.getBoolean("enabled", false);
            String display = own == null ? profession.id() : own.getString("display", profession.id());
            String region = own == null ? profession.defaultRegion()
                    : own.getString("region", profession.defaultRegion());
            double salary = own == null ? 0.0D : own.getDouble("salary-per-action", 0.0D);
            long xp = own == null ? 0L : own.getLong("xp-per-action", 0L);
            Set<Material> blocks = readMaterials(profession, own);
            professions.put(profession, new ProfessionSettings(profession, enabled, display, region,
                    Math.max(0.0D, salary), Math.max(0L, xp), blocks));
            if (enabled && region != null && !region.isBlank()) {
                Profession previous = regionIndex.put(region.toLowerCase(Locale.ROOT), profession);
                if (previous != null) {
                    warnings.add("config.yml: регион '" + region + "' назначен двум профессиям ("
                            + previous.id() + " и " + profession.id() + ").");
                }
            }
        }
        if (professions.values().stream().noneMatch(ProfessionSettings::enabled)) {
            warnings.add("config.yml: ни одна профессия не включена, награды начисляться не будут.");
        }
        if (section != null) {
            for (String key : section.getKeys(false)) {
                if (Profession.byId(key).isEmpty() && !"allow-change".equals(key)
                        && !"change-cooldown".equals(key)) {
                    warnings.add("config.yml: неизвестная профессия '" + key + "'.");
                }
            }
        }
    }

    private Set<Material> readMaterials(Profession profession, ConfigurationSection own) {
        List<String> configured = own == null ? List.of() : own.getStringList("blocks");
        if (configured.isEmpty()) {
            return ProfessionMaterials.defaults(profession);
        }
        Set<Material> materials = EnumSet.noneOf(Material.class);
        for (String name : configured) {
            Material material = Material.matchMaterial(name);
            if (material == null) {
                warnings.add("config.yml: professions." + profession.id()
                        + ".blocks содержит неизвестный материал '" + name + "'.");
            } else {
                materials.add(material);
            }
        }
        if (materials.isEmpty() && profession != Profession.FISH) {
            warnings.add("config.yml: у профессии " + profession.id()
                    + " пустой список блоков, использую значения по умолчанию.");
            return ProfessionMaterials.defaults(profession);
        }
        return materials;
    }

    private void readAntiExploit() {
        var yaml = configFile.yaml();
        antiExploit = new AntiExploitSettings(
                yaml.getBoolean("anti-exploit.require-region", true),
                yaml.getBoolean("anti-exploit.deny-creative", true),
                clamp(yaml.getInt("anti-exploit.min-action-interval-ms", 75), 0, 10_000),
                clamp(yaml.getInt("anti-exploit.max-actions-per-second", 8), 1, 200),
                yaml.getBoolean("anti-exploit.track-placed-blocks", true),
                TimeUtil.parseMillis(yaml.getString("anti-exploit.placed-block-ttl", "15m"), 900_000L),
                yaml.getString("anti-exploit.bypass-permission", AntiExploitSettings.DEFAULT_BYPASS),
                TimeUtil.parseMillis(yaml.getString("anti-exploit.buyer-cooldown", "1500ms"), 1500L),
                clamp(yaml.getInt("anti-exploit.max-buyer-sales-per-minute", 20), 1, 600)
        );
    }

    private void readLevels() {
        var yaml = configFile.yaml();
        Map<Integer, Double> rewards = new LinkedHashMap<>();
        ConfigurationSection section = yaml.getConfigurationSection("levels.rewards");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                int level = Numbers.parsePositive(key, -1);
                if (level > 0) {
                    rewards.put(level, section.getDouble(key, 0.0D));
                }
            }
        }
        levels = new LevelCurve(
                yaml.getBoolean("levels.enabled", true),
                clamp(yaml.getInt("levels.max-level", 100), 1, 10_000),
                Math.max(1.0D, yaml.getDouble("levels.base-xp", 100.0D)),
                Math.max(0.1D, yaml.getDouble("levels.exponent", 1.35D)),
                Math.max(0.0D, yaml.getDouble("levels.salary-per-level", 0.02D)),
                rewards
        );
    }

    private void readBuyer() {
        var yaml = configFile.yaml();
        buyer = new BuyerSettings(
                yaml.getBoolean("buyer.enabled", true),
                clamp(yaml.getInt("buyer.rows", 6), 3, 6),
                yaml.getString("buyer.title", "<dark_gray>Скупщик</dark_gray>"),
                TimeUtil.parseMillis(yaml.getString("buyer.cooldown", "1500ms"), 1500L),
                yaml.getBoolean("buyer.autosell-default", false)
        );
    }

    private void readBoosts() {
        ConfigurationSection section = boostsFile.yaml().getConfigurationSection("types");
        if (section == null) {
            warnings.add("boosts.yml: отсутствует секция types, бусты отключены.");
            return;
        }
        for (String key : section.getKeys(false)) {
            BoostType type = BoostType.byId(key);
            if (type == null) {
                warnings.add("boosts.yml: неизвестный тип буста '" + key + "'.");
                continue;
            }
            ConfigurationSection own = section.getConfigurationSection(key);
            if (own == null) {
                continue;
            }
            boosts.put(type, new BoostDefinition(
                    type,
                    own.getString("display", key),
                    Math.max(1.0D, own.getDouble("multiplier", 1.5D)),
                    TimeUtil.parseMillis(own.getString("duration", "30m"), 1_800_000L),
                    Math.max(0.0D, own.getDouble("price", 0.0D)),
                    own.getString("permission", "tradewind.boost." + type.id()),
                    own.getString("material", "GOLD_INGOT")
            ));
        }
    }

    private void readPrices() {
        ConfigurationSection section = pricesFile.yaml().getConfigurationSection("items");
        Map<Material, Double> prices = new EnumMap<>(Material.class);
        if (section == null) {
            warnings.add("prices.yml: отсутствует секция items, скупщик не сможет ничего купить.");
        } else {
            for (String key : section.getKeys(false)) {
                Material material = Material.matchMaterial(key);
                if (material == null || !material.isItem()) {
                    warnings.add("prices.yml: неизвестный или не-предмет '" + key + "'.");
                    continue;
                }
                double price = section.getDouble(key, -1.0D);
                if (price <= 0.0D) {
                    warnings.add("prices.yml: у '" + key + "' цена должна быть больше нуля.");
                    continue;
                }
                prices.put(material, price);
            }
        }
        this.prices = new PriceIndex(prices);
    }

    private void readShop() {
        var yaml = shopFile.yaml();
        Map<String, ShopItemDefinition> items = new LinkedHashMap<>();
        ConfigurationSection section = yaml.getConfigurationSection("items");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection own = section.getConfigurationSection(id);
                if (own == null) {
                    continue;
                }
                Material material = Material.matchMaterial(own.getString("material", ""));
                if (material == null || !material.isItem()) {
                    warnings.add("shop.yml: у '" + id + "' неизвестный материал.");
                    continue;
                }
                Profession required = own.getString("profession") == null
                        ? null : Profession.byId(own.getString("profession", "")).orElse(null);
                Map<String, Integer> enchantments = new LinkedHashMap<>();
                ConfigurationSection enchSection = own.getConfigurationSection("enchantments");
                if (enchSection != null) {
                    for (String enchantmentName : enchSection.getKeys(false)) {
                        int level = enchSection.getInt(enchantmentName, 1);
                        if (resolveEnchantment(enchantmentName) == null) {
                            warnings.add("shop.yml: у '" + id + "' неизвестный enchantment '"
                                    + enchantmentName + "'.");
                        } else {
                            enchantments.put(enchantmentName, level);
                        }
                    }
                }
                items.put(id, new ShopItemDefinition(
                        id, material,
                        clamp(own.getInt("amount", 1), 1, 64),
                        own.getString("display", id),
                        own.getStringList("lore"),
                        Math.max(0.0D, own.getDouble("price", 0.0D)),
                        required,
                        clamp(own.getInt("required-level", 0), 0, 10_000),
                        enchantments,
                        own.getBoolean("unbreakable", false),
                        own.getString("permission", "")
                ));
            }
        }
        shop = new ShopCatalog(
                yaml.getBoolean("enabled", true),
                clamp(yaml.getInt("rows", 5), 3, 6),
                yaml.getString("title", "<dark_gray>Уникальные крафты</dark_gray>"),
                items
        );
    }

    /**
     * Resolves modern keys ("efficiency") and the legacy names admins still copy
     * from old configs ("DIG_SPEED").
     */
    public static Enchantment resolveEnchantment(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalized = name.toLowerCase(Locale.ROOT).replace(' ', '_');
        Enchantment modern = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(normalized));
        if (modern != null) {
            return modern;
        }
        String alias = LEGACY_ENCHANTMENTS.get(normalized);
        return alias == null ? null : Registry.ENCHANTMENT.get(NamespacedKey.minecraft(alias));
    }

    private void validate() {
        if (currencyName == null || currencyName.isBlank()) {
            warnings.add("config.yml: economy.currency-name пуст, использую 'монеты'.");
            currencyName = "монеты";
        }
        if (prices.size() == 0) {
            warnings.add("prices.yml: нет ни одной цены.");
        }
        if (buyer.enabled() && prices.size() == 0) {
            warnings.add("config.yml: скупщик включён, но prices.yml пуст — GUI сможет только закрыться.");
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // --------------------------------------------------------------- accessors

    public int version() {
        return configFile.version();
    }

    public int pricesVersion() {
        return pricesFile.version();
    }

    public int boostsVersion() {
        return boostsFile.version();
    }

    public int shopVersion() {
        return shopFile.version();
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

    public StorageSettings storage() {
        return storage;
    }

    public Map<Profession, ProfessionSettings> professions() {
        return Collections.unmodifiableMap(professions);
    }

    public ProfessionSettings profession(Profession profession) {
        return professions.get(profession);
    }

    public Set<Profession> enabledProfessions() {
        Set<Profession> enabled = EnumSet.noneOf(Profession.class);
        professions.forEach((profession, settings) -> {
            if (settings.enabled()) {
                enabled.add(profession);
            }
        });
        return enabled;
    }

    public Profession professionByRegion(String regionId) {
        return regionId == null ? null : regionIndex.get(regionId.toLowerCase(Locale.ROOT));
    }

    public AntiExploitSettings antiExploit() {
        return antiExploit;
    }

    public LevelCurve levels() {
        return levels;
    }

    public BuyerSettings buyer() {
        return buyer;
    }

    public PriceIndex prices() {
        return prices;
    }

    public Map<BoostType, BoostDefinition> boosts() {
        return Collections.unmodifiableMap(boosts);
    }

    public BoostDefinition boost(BoostType type) {
        return boosts.get(type);
    }

    public ShopCatalog shop() {
        return shop;
    }

    public boolean allowProfessionChange() {
        return allowProfessionChange;
    }

    public long professionChangeCooldownMillis() {
        return professionChangeCooldownMillis;
    }
}

