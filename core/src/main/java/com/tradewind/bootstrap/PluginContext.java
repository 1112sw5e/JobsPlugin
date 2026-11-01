package com.tradewind.bootstrap;

import com.tradewind.TradewindPlugin;
import com.tradewind.command.TradewindCommand;
import com.tradewind.config.MainConfig;
import com.tradewind.config.Messages;
import com.tradewind.economy.EconomyService;
import com.tradewind.economy.UnavailableEconomy;
import com.tradewind.economy.VaultEconomyService;
import com.tradewind.platform.Platform;
import com.tradewind.platform.PlatformSupport;
import java.util.Optional;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;

/**
 * Owns everything that outlives a single command: configuration, messages, dependency state,
 * the economy bridge and the platform adapter. Stage 3 will add the storage layer here.
 */
public final class PluginContext {

    private final TradewindPlugin plugin;
    private final MainConfig config;
    private final Messages messages;
    private final Platform platform = new PlatformSupport();

    private DependencyReport dependencies;
    private EconomyService economy = UnavailableEconomy.INSTANCE;

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
        registerCommand();
        logStartup();

        if (dependencies.satisfied()) {
            return;
        }
        String missingNames = DependencyReport.names(dependencies.missingRequired());
        if (config.failFast()) {
            throw new PluginStartupException("Tradewind выключен: отсутствуют обязательные зависимости — "
                    + missingNames + ". Установите их или включите dependencies.fail-fast=false в config.yml "
                    + "для запуска в урезанном режиме.");
        }
        plugin.getLogger().warning("Tradewind: отсутствуют обязательные зависимости — " + missingNames
                + ". Работаю в урезанном режиме (dependencies.fail-fast=false).");
        messages.send(plugin.getServer().getConsoleSender(), "startup.degraded",
                "missing", missingNames);
    }

    /** Reloads configs, re-scans dependencies and reconnects the economy. */
    public Optional<String> reload() {
        try {
            config.reload();
            messages.reload();
            reportConfigWarnings();
            dependencies = DependencyReport.scan(plugin.getServer().getPluginManager());
            economy = connectEconomy();
            return Optional.empty();
        } catch (RuntimeException failure) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка перезагрузки Tradewind", failure);
            String reason = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
            return Optional.of(reason);
        }
    }

    /** Releases resources. Placeholder for stage 3, where the storage layer is closed here. */
    public void close() {
        // nothing to release yet
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

    private void registerCommand() {
        PluginCommand command = plugin.getCommand("tradewind");
        if (command == null) {
            plugin.getLogger().severe("Команда tradewind не объявлена в plugin.yml, команды не работают.");
            return;
        }
        TradewindCommand executor = new TradewindCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void logStartup() {
        plugin.getLogger().info("Tradewind v" + version() + ": сервер " + platform.minecraftVersion()
                + " [" + platform.runningLine().label() + "], сборка " + platform.artifactLabel()
                + ", профессии " + config.enabledProfessions());
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
                    + "Баланс, зарплата и сдача предметов работать не будут.");
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
}

