package com.tradewind.command;

import com.tradewind.bootstrap.Dependency;
import com.tradewind.bootstrap.PluginContext;
import com.tradewind.config.Messages;
import com.tradewind.model.BoostType;
import com.tradewind.model.LeaderboardEntry;
import com.tradewind.model.Profession;
import com.tradewind.model.PlayerData;
import com.tradewind.platform.Platform;
import com.tradewind.util.Numbers;
import com.tradewind.util.TimeUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/** {@code /tradewind help|status|reload}. */
public final class TradewindCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION_STATUS = "tradewind.admin.status";
    private static final String PERMISSION_RELOAD = "tradewind.admin.reload";
    private static final String PERMISSION_STATS = "tradewind.admin.stats";
    private static final String PERMISSION_BOOST = "tradewind.admin.boost";
    private static final List<String> SUBCOMMANDS =
            List.of("help", "status", "reload", "top", "stats", "boost");

    private final PluginContext context;

    public TradewindCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String subcommand = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "help" -> sendHelp(sender, label);
            case "status" -> sendStatus(sender);
            case "reload" -> reload(sender);
            case "top" -> sendTop(sender, args);
            case "stats" -> sendStats(sender, args);
            case "boost" -> grantBoost(sender, args);
            default -> context.messages().send(sender, "command.unknown-subcommand",
                    "input", args[0], "label", label);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>(SUBCOMMANDS.size());
        for (String candidate : SUBCOMMANDS) {
            if (candidate.startsWith(prefix) && canUse(sender, candidate)) {
                suggestions.add(candidate);
            }
        }
        return suggestions;
    }

    private void sendHelp(CommandSender sender, String label) {
        Messages messages = context.messages();
        Platform platform = context.platform();
        messages.send(sender, "command.help.header", "version", context.version());
        for (String subcommand : SUBCOMMANDS) {
            if (canUse(sender, subcommand)) {
                messages.send(sender, "command.help.line",
                        "label", label,
                        "sub", subcommand,
                        "description", messages.raw("command.help.sub." + subcommand));
            }
        }
        messages.send(sender, "command.help.usage",
                "artifact", platform.artifactLabel(),
                "server", platform.minecraftVersion());
    }

    private void sendStatus(CommandSender sender) {
        if (!requirePermission(sender, PERMISSION_STATUS)) {
            return;
        }
        Messages messages = context.messages();
        Platform platform = context.platform();

        messages.send(sender, "command.status.header");
        messages.send(sender, "command.status.version", "version", context.version());
        messages.send(sender, "command.status.server",
                "minecraft", platform.minecraftVersion(),
                "line", platform.runningLine().label());
        if (platform.matches()) {
            messages.send(sender, "command.status.artifact-ok", "artifact", platform.artifactLabel());
        } else {
            messages.send(sender, "command.status.artifact-mismatch",
                    "artifact", platform.artifactLabel(),
                    "expected", platform.expectedLine().label());
        }

        messages.send(sender, "command.status.dependencies-header");
        for (Dependency dependency : Dependency.values()) {
            String key;
            if (context.dependencies().isPresent(dependency)) {
                key = "command.status.dependency-present";
            } else if (dependency.required()) {
                key = "command.status.dependency-missing-required";
            } else {
                key = "command.status.dependency-missing-optional";
            }
            messages.send(sender, key, "name", dependency.pluginName(), "purpose", dependency.purpose());
        }

        if (context.economy().available()) {
            String balance = sender instanceof Player player
                    ? context.economy().format(context.economy().balance(player))
                    : "—";
            messages.send(sender, "command.status.economy",
                    "backend", context.economy().backend(),
                    "balance", balance);
        } else {
            messages.send(sender, "command.status.economy-unavailable");
        }
        messages.send(sender, "command.status.config-versions",
                "config", String.valueOf(context.config().version()),
                "messages", String.valueOf(messages.version()));
    }

    private void reload(CommandSender sender) {
        if (!requirePermission(sender, PERMISSION_RELOAD)) {
            return;
        }
        Messages messages = context.messages();
        Optional<String> failure = context.reload();
        if (failure.isPresent()) {
            messages.send(sender, "command.reload.failed", "error", failure.get());
        } else {
            messages.send(sender, "command.reload.success",
                    "dependencies", context.dependencies().summary());
        }
    }

    private boolean requirePermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        context.messages().send(sender, "command.no-permission");
        return false;
    }

    private boolean canUse(CommandSender sender, String subcommand) {
        return switch (subcommand) {
            case "status" -> sender.hasPermission(PERMISSION_STATUS);
            case "reload" -> sender.hasPermission(PERMISSION_RELOAD);
            case "stats" -> sender.hasPermission(PERMISSION_STATS);
            case "boost" -> sender.hasPermission(PERMISSION_BOOST);
            default -> true;
        };
    }

    private void sendTop(CommandSender sender, String[] args) {
        Profession profession;
        if (args.length >= 2) {
            Optional<Profession> parsed = Profession.byId(args[1]);
            if (parsed.isEmpty()) {
                context.messages().send(sender, "profession.unknown", "input", args[1]);
                return;
            }
            profession = parsed.get();
        } else if (sender instanceof Player player
                && context.players().get(player) != null
                && context.players().get(player).profession().isPresent()) {
            profession = context.players().get(player).profession().get();
        } else {
            context.messages().send(sender, "command.usage-top");
            return;
        }
        var settings = context.config().profession(profession);
        context.messages().send(sender, "top.header",
                "profession", settings == null ? profession.id() : settings.display());
        List<LeaderboardEntry> entries = context.leaderboards().top(profession);
        if (entries.isEmpty()) {
            context.messages().send(sender, "top.empty");
            return;
        }
        int rank = 1;
        for (LeaderboardEntry entry : entries) {
            context.messages().send(sender, "top.line",
                    "rank", String.valueOf(rank++),
                    "name", entry.name(),
                    "actions", String.valueOf(entry.actions()),
                    "money", context.economy().format(entry.money()),
                    "level", String.valueOf(entry.level()));
        }
    }

    private void sendStats(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION_STATS)) {
            context.messages().send(sender, "command.no-permission");
            return;
        }
        PlayerData data;
        String targetName;
        if (args.length >= 2) {
            var target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                context.messages().send(sender, "command.player-not-found", "player", args[1]);
                return;
            }
            data = context.players().get(target);
            targetName = target.getName();
        } else if (sender instanceof Player player) {
            data = context.players().get(player);
            targetName = player.getName();
        } else {
            context.messages().send(sender, "command.usage-stats");
            return;
        }
        if (data == null) {
            context.messages().send(sender, "profile-loading");
            return;
        }
        context.messages().send(sender, "admin.stats.header", "player", targetName);
        for (Profession profession : Profession.values()) {
            var stats = data.stats(profession);
            if (stats.actions() == 0L && stats.xp() == 0L) {
                continue;
            }
            context.messages().send(sender, "admin.stats.line",
                    "profession", profession.id(),
                    "level", String.valueOf(stats.level()),
                    "xp", String.valueOf(stats.xp()),
                    "actions", String.valueOf(stats.actions()),
                    "money", context.economy().format(stats.money()));
        }
        context.messages().send(sender, "admin.stats.footer",
                "autosell", data.autoSell() ? "вкл" : "выкл",
                "money", context.economy().format(data.totalMoney()),
                "actions", String.valueOf(data.totalActions()));
    }

    private void grantBoost(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION_BOOST)) {
            context.messages().send(sender, "command.no-permission");
            return;
        }
        if (args.length < 3) {
            context.messages().send(sender, "command.usage-boost");
            return;
        }
        var target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            context.messages().send(sender, "command.player-not-found", "player", args[1]);
            return;
        }
        BoostType type = BoostType.byId(args[2]);
        if (type == null) {
            context.messages().send(sender, "command.usage-boost");
            return;
        }
        long duration = TimeUtil.parseMillis(args.length >= 4 ? args[3] : "30m", 1_800_000L);
        PlayerData data = context.players().get(target);
        if (data == null || !context.boosts().grant(data, type, duration)) {
            context.messages().send(sender, "profile-loading");
            return;
        }
        Map<String, String> values = Map.of(
                "player", target.getName(),
                "type", type.id(),
                "duration", TimeUtil.format(duration),
                "multiplier", Numbers.fixed(
                        context.config().boost(type) == null ? 1.0D
                                : context.config().boost(type).multiplier(), 2));
        context.messages().send(sender, "admin.boost.granted", values.entrySet().stream()
                .flatMap(entry -> java.util.stream.Stream.of(entry.getKey(), entry.getValue()))
                .toArray(String[]::new));
        context.messages().send(target, "boost.received",
                "type", type.id(), "duration", TimeUtil.format(duration));
    }
}

