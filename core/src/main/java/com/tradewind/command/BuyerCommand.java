package com.tradewind.command;

import com.tradewind.bootstrap.PluginContext;
import com.tradewind.gui.BuyerGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /buyer} — open the collector interface. */
public final class BuyerCommand implements CommandExecutor {

    private final PluginContext context;

    public BuyerCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            context.messages().send(sender, "command.player-only");
            return true;
        }
        if (!player.hasPermission("tradewind.buyer")) {
            context.messages().send(player, "command.no-permission");
            return true;
        }
        if (!context.config().buyer().enabled()) {
            context.messages().send(player, "buyer.disabled");
            return true;
        }
        if (context.players().get(player) == null) {
            context.messages().send(player, "profile-loading");
            return true;
        }
        new BuyerGui(context, player).open();
        return true;
    }
}

