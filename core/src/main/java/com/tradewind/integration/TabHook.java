package com.tradewind.integration;

import com.tradewind.bootstrap.PluginContext;
import com.tradewind.model.Profession;
import com.tradewind.model.PlayerData;
import com.tradewind.util.Numbers;
import com.tradewind.util.TimeUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Registers TAB placeholders so a TAB scoreboard can use {@code %tradewind_level%} and friends.
 * Loaded only when TAB is present; all calls are guarded because TAB API versions drift.
 */
public final class TabHook {

    private final PluginContext context;
    private final List<String> registered = new ArrayList<>();

    private TabHook(PluginContext context) {
        this.context = context;
    }

    public static TabHook create(PluginContext context) {
        try {
            TabHook hook = new TabHook(context);
            hook.register();
            context.plugin().getLogger().info("TAB подключён: плейсхолдеры %tradewind_*% доступны.");
            return hook;
        } catch (LinkageError | RuntimeException failure) {
            context.plugin().getLogger().log(Level.WARNING,
                    "TAB найден, но его API не совместим; интеграция пропущена. "
                            + "Используйте плейсхолдеры PlaceholderAPI.", failure);
            return null;
        }
    }

    private void register() {
        var manager = TabAPI.getInstance().getPlaceholderManager();
        register(manager, "tradewind_profession", this::profession);
        register(manager, "tradewind_level", player -> String.valueOf(level(player)));
        register(manager, "tradewind_actions", player -> String.valueOf(actions(player)));
        register(manager, "tradewind_money", this::money);
        register(manager, "tradewind_boost", this::boost);
    }

    private void register(me.neznamy.tab.api.placeholder.PlaceholderManager manager,
                          String identifier,
                          java.util.function.Function<TabPlayer, String> resolver) {
        manager.registerPlayerPlaceholder("%" + identifier + "%", 500, resolver::apply);
        registered.add("%" + identifier + "%");
    }

    private String profession(TabPlayer tabPlayer) {
        PlayerData data = data(tabPlayer);
        if (data == null) {
            return "—";
        }
        return data.profession().map(profession -> {
            var settings = context.config().profession(profession);
            return settings == null ? profession.id() : settings.display();
        }).orElse("—");
    }

    private int level(TabPlayer tabPlayer) {
        PlayerData data = data(tabPlayer);
        return data == null ? 1
                : data.profession().map(profession -> data.stats(profession).level()).orElse(1);
    }

    private long actions(TabPlayer tabPlayer) {
        PlayerData data = data(tabPlayer);
        if (data == null || data.profession().isEmpty()) {
            return 0L;
        }
        return data.stats(data.profession().get()).actions();
    }

    private String money(TabPlayer tabPlayer) {
        PlayerData data = data(tabPlayer);
        return data == null ? "0" : context.economy().format(data.totalMoney());
    }

    private String boost(TabPlayer tabPlayer) {
        PlayerData data = data(tabPlayer);
        if (data == null) {
            return "—";
        }
        return context.boosts().active(data, com.tradewind.model.BoostType.MONEY)
                .map(active -> "x" + Numbers.fixed(active.multiplier(), 2)
                        + " " + TimeUtil.format(active.remainingMillis(System.currentTimeMillis())))
                .orElse("—");
    }

    private PlayerData data(TabPlayer tabPlayer) {
        try {
            Player player = Bukkit.getPlayer(tabPlayer.getUniqueId());
            return player == null ? null : context.players().get(player);
        } catch (RuntimeException failure) {
            return null;
        }
    }

    public void unregister() {
        try {
            var manager = TabAPI.getInstance().getPlaceholderManager();
            for (String identifier : registered) {
                manager.unregisterPlaceholder(identifier);
            }
        } catch (LinkageError | RuntimeException ignored) {
            // TAB is going away with the server; nothing to clean up.
        }
        registered.clear();
    }

    /** Exposed for diagnostics. */
    public List<String> placeholders() {
        return List.copyOf(registered);
    }

    /** Not used yet, kept so profession names stay accessible from a single place. */
    public static String professionId(Profession profession) {
        return profession.id();
    }
}

