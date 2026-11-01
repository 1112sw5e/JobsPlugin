package com.tradewind.command;

import com.tradewind.bootstrap.Dependency;
import com.tradewind.bootstrap.PluginContext;
import com.tradewind.config.Messages;
import com.tradewind.platform.Platform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/** {@code /tradewind help|status|reload}. */
public final class TradewindCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION_STATUS = "tradewind.admin.status";
    private static final String PERMISSION_RELOAD = "tradewind.admin.reload";
    private static final List<String> SUBCOMMANDS = List.of("help", "status", "reload");

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
            default -> true;
        };
    }
}

