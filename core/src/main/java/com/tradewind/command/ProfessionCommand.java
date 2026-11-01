package com.tradewind.command;

import com.tradewind.bootstrap.PluginContext;
import com.tradewind.config.LevelCurve;
import com.tradewind.config.ProfessionSettings;
import com.tradewind.model.Profession;
import com.tradewind.model.PlayerData;
import com.tradewind.service.ProfessionSelectionService;
import com.tradewind.util.TimeUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/** {@code /prof [профессия]} — view statistics or pick a job. */
public final class ProfessionCommand implements CommandExecutor, TabCompleter {

    private final PluginContext context;

    public ProfessionCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            context.messages().send(sender, "command.player-only");
            return true;
        }
        if (!player.hasPermission("tradewind.profession")) {
            context.messages().send(player, "command.no-permission");
            return true;
        }
        if (args.length == 0) {
            show(player);
            return true;
        }
        var profession = Profession.byId(args[0]);
        if (profession.isEmpty()) {
            context.messages().send(player, "profession.unknown", "input", args[0]);
            return true;
        }
        ProfessionSelectionService.Selection selection =
                context.selection().select(player, profession.get());
        switch (selection.result()) {
            case SELECTED -> {
                context.messages().send(player, "profession.selected",
                        "profession", selection.settings().display());
                show(player);
            }
            case SAME -> context.messages().send(player, "profession.already",
                    "profession", selection.settings().display());
            case DISABLED -> context.messages().send(player, "profession.disabled",
                    "profession", profession.get().id());
            case NOT_LOADED -> context.messages().send(player, "profile-loading");
            case CHANGE_FORBIDDEN -> context.messages().send(player, "profession.change-forbidden");
            case COOLDOWN -> context.messages().send(player, "profession.cooldown",
                    "time", TimeUtil.format(selection.remainingMillis()));
        }
        return true;
    }

    private void show(Player player) {
        PlayerData data = context.players().get(player);
        if (data == null) {
            context.messages().send(player, "profile-loading");
            return;
        }
        context.messages().send(player, "profession.info.header");
        if (data.profession().isEmpty()) {
            context.messages().send(player, "profession.not-selected");
            return;
        }
        Profession profession = data.profession().get();
        ProfessionSettings settings = context.config().profession(profession);
        LevelCurve curve = context.config().levels();
        var stats = data.stats(profession);
        Map<String, String> values = Map.of(
                "profession", settings.display(),
                "level", String.valueOf(stats.level()),
                "xp", String.valueOf(stats.xp()),
                "xp_next", String.valueOf(curve.xpToNextLevel(stats.level())),
                "progress", String.valueOf(curve.progressPercent(stats.xp(), stats.level())),
                "actions", String.valueOf(stats.actions()),
                "money", context.economy().format(stats.money()),
                "multiplier", String.format(Locale.ROOT, "x%.2f",
                        curve.salaryMultiplier(stats.level())));
        line(player, "level", values.get("level"));
        line(player, "xp", values.get("xp") + " / " + values.get("xp_next")
                + " (" + values.get("progress") + "%)");
        line(player, "actions", values.get("actions"));
        line(player, "money", values.get("money"));
        line(player, "multiplier", values.get("multiplier"));
        context.messages().send(player, "profession.info.bar",
                "progress", String.valueOf(curve.progressPercent(stats.xp(), stats.level())));
    }

    private void line(Player player, String key, String value) {
        context.messages().send(player, "profession.info.line",
                "key", context.messages().raw("profession.info.key." + key),
                "value", value);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();
        for (Profession profession : context.config().enabledProfessions()) {
            if (profession.id().startsWith(prefix)) {
                suggestions.add(profession.id());
            }
        }
        return suggestions;
    }
}

