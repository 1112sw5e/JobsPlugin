package com.tradewind.command;

import com.tradewind.bootstrap.PluginContext;
import com.tradewind.model.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /autosell [on|off]} — sell drops as they are earned. */
public final class AutoSellCommand implements CommandExecutor {

    private final PluginContext context;

    public AutoSellCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            context.messages().send(sender, "command.player-only");
            return true;
        }
        if (!player.hasPermission("tradewind.autosell")) {
            context.messages().send(player, "command.no-permission");
            return true;
        }
        if (!context.config().buyer().enabled()) {
            context.messages().send(player, "buyer.disabled");
            return true;
        }
        PlayerData data = context.players().get(player);
        if (data == null) {
            context.messages().send(player, "profile-loading");
            return true;
        }
        boolean target = args.length == 0 ? !data.autoSell() : parse(args[0], data.autoSell());
        data.autoSell(target);
        context.messages().send(player, target ? "autosell.enabled" : "autosell.disabled");
        return true;
    }

    private boolean parse(String input, boolean fallback) {
        return switch (input.toLowerCase(java.util.Locale.ROOT)) {
            case "on", "true", "вкл", "yes" -> true;
            case "off", "false", "выкл", "no" -> false;
            default -> fallback;
        };
    }
}

