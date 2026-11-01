package com.tradewind.bootstrap;

import com.tradewind.TradewindPlugin;
import com.tradewind.anti.ActionLimiter;
import com.tradewind.anti.PlacedBlockTracker;
import com.tradewind.boost.BoostService;
import com.tradewind.buyer.BuyerService;
import com.tradewind.command.AutoSellCommand;
import com.tradewind.command.BoostCommand;
import com.tradewind.command.BuyerCommand;
import com.tradewind.command.ProfessionCommand;
import com.tradewind.command.ShopCommand;
import com.tradewind.command.TradewindCommand;
import com.tradewind.config.MainConfig;
import com.tradewind.config.Messages;
import com.tradewind.economy.EconomyService;
import com.tradewind.economy.UnavailableEconomy;
import com.tradewind.economy.VaultEconomyService;
import com.tradewind.gui.GuiListener;
import com.tradewind.integration.PlaceholderHook;
import com.tradewind.integration.TabHook;
import com.tradewind.item.CustomItems;
import com.tradewind.listener.BlockListener;
import com.tradewind.listener.FishingListener;
import com.tradewind.listener.SessionListener;
import com.tradewind.model.PlayerData;
import com.tradewind.platform.Platform;
import com.tradewind.platform.PlatformSupport;
import com.tradewind.profession.ProfessionService;
import com.tradewind.region.DisabledRegions;
import com.tradewind.region.RegionService;
import com.tradewind.region.WorldGuardRegions;
import com.tradewind.service.LeaderboardService;
import com.tradewind.service.ProfessionSelectionService;
import com.tradewind.shop.ShopService;
import com.tradewind.storage.DbExecutor;
import com.tradewind.storage.JdbcStorage;
import com.tradewind.storage.StorageException;
import com.tradewind.storage.PlayerRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Composition root: builds and owns every long-lived service, then tears them down.
 * The plugin class itself stays thin so startup failures have exactly one reporting path.
 */
public final class PluginContext {

    private final TradewindPlugin plugin;
    private final MainConfig config;
    private final Messages messages;
    private final Platform platform = new PlatformSupport();
    private final List<BukkitTask> tasks = new ArrayList<>();

    private DependencyReport dependencies;
    private EconomyService economy = UnavailableEconomy.INSTANCE;
    private RegionService regions = DisabledRegions.INSTANCE;
    private CustomItems items;
    private PlayerRepository players;
    private DbExecutor dbExecutor;
    private PlacedBlockTracker placedBlocks;
    private ActionLimiter limiter;
    private ProfessionService professions;
    private BuyerService buyer;
    private BoostService boosts;
    private ShopService shop;
    private LeaderboardService leaderboards;
    private ProfessionSelectionService selection;
    private PlaceholderHook placeholderHook;
    private TabHook tabHook;

    private PluginContext(TradewindPlugin plugin) {
        this.plugin = plugin;
        this.config = new MainConfig(plugin);
        this.messages = new Messages(plugin);
    }

    public static PluginContext start(TradewindPlugin plugin) throws PluginStartupException {
        PluginContext context = new PluginContext(plugin);
        context.load();
        return context;
    }

    private void load() throws PluginStartupException {
        config.reload();
        messages.reload();
        reportConfigWarnings();

        dependencies = DependencyReport.scan(plugin.getServer().getPluginManager());
        economy = connectEconomy();
        regions = connectRegions();
        openStorage();

        items = new CustomItems(plugin);
        placedBlocks = new PlacedBlockTracker(config.antiExploit().placedBlockTtlMillis());
        limiter = new ActionLimiter(config.antiExploit().minActionIntervalMillis(),
                config.antiExploit().maxActionsPerSecond());
        professions = new ProfessionService(this);
        buyer = new BuyerService(this);
        boosts = new BoostService(this);
        shop = new ShopService(this);
        leaderboards = new LeaderboardService(this);
        selection = new ProfessionSelectionService(this);

        registerListeners();
        registerCommands();
        scheduleTasks();
        leaderboards.refreshAsync();
        enableIntegrations();
        logStartup();
        enforceDependencies();
    }

    private void openStorage() throws PluginStartupException {
        JdbcStorage storage = new JdbcStorage(config.storage(), plugin.getDataFolder().toPath(),
                plugin.getLogger());
        try {
            storage.init();
        } catch (StorageException failure) {
            throw new PluginStartupException("Tradewind выключен: " + failure.getMessage()
                    + " Проверьте секцию storage в config.yml.", failure);
        }
        dbExecutor = new DbExecutor(plugin.getLogger());
        players = new PlayerRepository(plugin.getLogger(), storage, dbExecutor,
                config.buyer().autoSellDefault());
    }

    private EconomyService connectEconomy() {
        if (!dependencies.isPresent(Dependency.VAULT)) {
            return UnavailableEconomy.INSTANCE;
        }
        try {
            return VaultEconomyService.create(plugin.getLogger());
        } catch (LinkageError brokenVault) {
            plugin.getLogger().log(Level.SEVERE,
                    "Vault найден, но его API не загрузился; экономика отключена.", brokenVault);
            return UnavailableEconomy.INSTANCE;
        }
    }

    private RegionService connectRegions() {
        if (!dependencies.isPresent(Dependency.WORLD_GUARD)) {
            return DisabledRegions.INSTANCE;
        }
        try {
            return WorldGuardRegions.create(plugin.getLogger());
        } catch (LinkageError brokenWorldGuard) {
            plugin.getLogger().log(Level.SEVERE,
                    "WorldGuard найден, но его API не загрузился; регионы отключены.", brokenWorldGuard);
            return DisabledRegions.INSTANCE;
        }
    }

    private void registerListeners() {
        var manager = plugin.getServer().getPluginManager();
        manager.registerEvents(new BlockListener(this), plugin);
        manager.registerEvents(new FishingListener(this), plugin);
        manager.registerEvents(new SessionListener(this), plugin);
        manager.registerEvents(new GuiListener(), plugin);
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            players.loadAsync(online);
        }
    }

    private void registerCommands() {
        register("tradewind", new TradewindCommand(this));
        register("prof", new ProfessionCommand(this));
        register("buyer", new BuyerCommand(this));
        register("autosell", new AutoSellCommand(this));
        register("boost", new BoostCommand(this));
        register("shop", new ShopCommand(this));
    }

    private void register(String name, CommandExecutor executor) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            plugin.getLogger().severe("Команда " + name + " не объявлена в plugin.yml.");
            return;
        }
        command.setExecutor(executor);
        if (executor instanceof TabCompleter completer) {
            command.setTabCompleter(completer);
        }
    }

    private void scheduleTasks() {
        var scheduler = plugin.getServer().getScheduler();
        long flushTicks = Math.max(5, config.storage().flushIntervalSeconds()) * 20L;
        tasks.add(scheduler.runTaskTimer(plugin, this::flushNow, flushTicks, flushTicks));
        tasks.add(scheduler.runTaskTimer(plugin, () -> placedBlocks.prune(), 1200L, 1200L));
        tasks.add(scheduler.runTaskTimer(plugin, this::expireBoosts, 1200L, 1200L));
        tasks.add(scheduler.runTaskTimer(plugin, () -> leaderboards.refreshAsync(), 600L, 6000L));
    }

    private void flushNow() {
        int flushed = players.flushDirty();
        if (flushed > 0) {
            debug("сохранено профилей: " + flushed);
        }
    }

    private void expireBoosts() {
        long now = System.currentTimeMillis();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PlayerData data = players.get(player);
            if (data != null) {
                data.pruneExpiredBoosts(now);
            }
        }
    }

    private void enableIntegrations() {
        if (dependencies.isPresent(Dependency.PLACEHOLDER_API)) {
            try {
                placeholderHook = new PlaceholderHook(this);
                if (!placeholderHook.register()) {
                    plugin.getLogger().warning("PlaceholderAPI найдена, но расширение не зарегистрировалось.");
                    placeholderHook = null;
                }
            } catch (LinkageError | RuntimeException failure) {
                plugin.getLogger().log(Level.WARNING,
                        "PlaceholderAPI найдена, но расширение не загрузилось.", failure);
                placeholderHook = null;
            }
        }
        if (dependencies.isPresent(Dependency.TAB)) {
            tabHook = TabHook.create(this);
        }
    }

    private void disableIntegrations() {
        if (placeholderHook != null) {
            try {
                placeholderHook.unregister();
            } catch (LinkageError | RuntimeException failure) {
                plugin.getLogger().log(Level.FINE, "Не удалось снять расширение PlaceholderAPI.", failure);
            }
            placeholderHook = null;
        }
        if (tabHook != null) {
            tabHook.unregister();
            tabHook = null;
        }
    }

    private void logStartup() {
        plugin.getLogger().info("Tradewind v" + version() + ": сервер " + platform.minecraftVersion()
                + " [" + platform.runningLine().label() + "], сборка " + platform.artifactLabel()
                + ", профессии " + config.enabledProfessions().stream()
                .map(com.tradewind.model.Profession::id).toList());
        plugin.getLogger().info("Хранилище: " + players.describe()
                + ", цен в prices.yml: " + config.prices().size()
                + ", товаров в shop.yml: " + config.shop().items().size());
        if (!platform.matches()) {
            plugin.getLogger().warning("Сборка рассчитана на " + platform.expectedLine().label()
                    + ", а сервер сообщает " + platform.runningLine().label()
                    + ". Проверьте, что установлен jar для этой линейки.");
        }
        if (!dependencies.missingRequired().isEmpty()) {
            plugin.getLogger().warning("Нет обязательных зависимостей: "
                    + DependencyReport.names(dependencies.missingRequired()));
        }
        if (config.warnOnMissingOptional() && !dependencies.missingOptional().isEmpty()) {
            plugin.getLogger().info("Мягкие зависимости не найдены: "
                    + DependencyReport.names(dependencies.missingOptional()) + " (не критично).");
        }
        if (economy.available()) {
            plugin.getLogger().info("Экономика Vault подключена: " + economy.backend());
        } else {
            plugin.getLogger().warning("Экономика недоступна: Vault или его провайдер не найдены. "
                    + "Награды и скупщик работать не будут.");
        }
        if (!regions.available()) {
            plugin.getLogger().warning("Регионы WorldGuard недоступны: награды профессий не начисляются.");
        }
    }

    private void enforceDependencies() throws PluginStartupException {
        if (dependencies.satisfied()) {
            return;
        }
        String missingNames = DependencyReport.names(dependencies.missingRequired());
        if (config.failFast()) {
            throw new PluginStartupException("Tradewind выключен: отсутствуют обязательные зависимости — "
                    + missingNames + ". Установите их или включите dependencies.fail-fast=false "
                    + "в config.yml для запуска в урезанном режиме.");
        }
        plugin.getLogger().warning("Tradewind: отсутствуют обязательные зависимости — " + missingNames
                + ". Работаю в урезанном режиме (dependencies.fail-fast=false).");
        messages.send(plugin.getServer().getConsoleSender(), "startup.degraded", "missing", missingNames);
    }

    /** Reloads configs and reconnects every replaceable dependency. */
    public Optional<String> reload() {
        try {
            String storageBefore = config.storage().toString();
            config.reload();
            messages.reload();
            reportConfigWarnings();

            dependencies = DependencyReport.scan(plugin.getServer().getPluginManager());
            economy = connectEconomy();
            regions = connectRegions();
            placedBlocks = new PlacedBlockTracker(config.antiExploit().placedBlockTtlMillis());
            limiter = new ActionLimiter(config.antiExploit().minActionIntervalMillis(),
                    config.antiExploit().maxActionsPerSecond());
            buyer = new BuyerService(this);
            disableIntegrations();
            enableIntegrations();
            leaderboards.refreshAsync();

            if (!storageBefore.equals(config.storage().toString())) {
                plugin.getLogger().warning("storage в config.yml изменён (" + storageBefore + " -> "
                        + config.storage() + "). Новое хранилище применится после перезапуска сервера.");
            }
            plugin.getLogger().info("Конфиги перезагружены. Зависимости: " + dependencies.summary());
            return Optional.empty();
        } catch (RuntimeException failure) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка перезагрузки Tradewind", failure);
            String reason = failure.getMessage() == null
                    ? failure.getClass().getSimpleName() : failure.getMessage();
            return Optional.of(reason);
        }
    }

    public void close() {
        for (BukkitTask task : tasks) {
            if (!task.isCancelled()) {
                task.cancel();
            }
        }
        tasks.clear();
        disableIntegrations();
        if (players != null) {
            players.close();
        }
    }

    private void reportConfigWarnings() {
        for (String warning : config.warnings()) {
            plugin.getLogger().warning(warning);
        }
    }

    public void debug(String message) {
        if (config.debug()) {
            plugin.getLogger().info("[debug] " + message);
        }
    }

    public TradewindPlugin plugin() {
        return plugin;
    }

    public String version() {
        return plugin.getPluginMeta().getVersion();
    }

    public MainConfig config() {
        return config;
    }

    public Messages messages() {
        return messages;
    }

    public Platform platform() {
        return platform;
    }

    public DependencyReport dependencies() {
        return dependencies;
    }

    public EconomyService economy() {
        return economy;
    }

    public RegionService regions() {
        return regions;
    }

    public PlayerRepository players() {
        return players;
    }

    public PlacedBlockTracker placedBlocks() {
        return placedBlocks;
    }

    public ActionLimiter limiter() {
        return limiter;
    }

    public ProfessionService professions() {
        return professions;
    }

    public BuyerService buyer() {
        return buyer;
    }

    public BoostService boosts() {
        return boosts;
    }

    public ShopService shop() {
        return shop;
    }

    public CustomItems items() {
        return items;
    }

    public LeaderboardService leaderboards() {
        return leaderboards;
    }

    public ProfessionSelectionService selection() {
        return selection;
    }
}

