package com.tradewind.command;

import com.tradewind.bootstrap.PluginContext;
import com.tradewind.gui.BoostGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /boost} — open the boost purchase interface. */
public final class BoostCommand implements CommandExecutor {

    private final PluginContext context;

    public BoostCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            context.messages().send(sender, "command.player-only");
            return true;
        }
        if (!player.hasPermission("tradewind.boost")) {
            context.messages().send(player, "command.no-permission");
            return true;
        }
        if (context.config().boosts().isEmpty()) {
            context.messages().send(player, "boost.disabled");
            return true;
        }
        if (context.players().get(player) == null) {
            context.messages().send(player, "profile-loading");
            return true;
        }
        new BoostGui(context, player).open();
        return true;
    }
}

