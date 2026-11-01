package com.tradewind.command;

import com.tradewind.bootstrap.PluginContext;
import com.tradewind.gui.ShopGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /shop} — open the unique crafts store. */
public final class ShopCommand implements CommandExecutor {

    private final PluginContext context;

    public ShopCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            context.messages().send(sender, "command.player-only");
            return true;
        }
        if (!player.hasPermission("tradewind.shop")) {
            context.messages().send(player, "command.no-permission");
            return true;
        }
        if (!context.config().shop().enabled()) {
            context.messages().send(player, "shop.disabled");
            return true;
        }
        if (context.players().get(player) == null) {
            context.messages().send(player, "profile-loading");
            return true;
        }
        new ShopGui(context, player).open();
        return true;
    }
}

