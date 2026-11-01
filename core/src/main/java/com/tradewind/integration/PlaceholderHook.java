package com.tradewind.integration;

import com.tradewind.bootstrap.PluginContext;
import com.tradewind.config.ProfessionSettings;
import com.tradewind.model.LeaderboardEntry;
import com.tradewind.model.Profession;
import com.tradewind.model.PlayerData;
import com.tradewind.text.Text;
import com.tradewind.util.Numbers;
import com.tradewind.util.TimeUtil;
import java.util.Locale;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion. Loaded only when PlaceholderAPI is installed; the class
 * itself extends a PAPI type, so it must never be touched otherwise.
 */
public final class PlaceholderHook extends PlaceholderExpansion {

    private final PluginContext context;

    public PlaceholderHook(PluginContext context) {
        this.context = context;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "tradewind";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Tradewind";
    }

    @Override
    public @NotNull String getVersion() {
        return context.version();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null) {
            return "";
        }
        PlayerData data = context.players().get(offlinePlayer.getUniqueId());
        String key = params.toLowerCase(Locale.ROOT);

        if ("total_money".equals(key)) {
            return data == null ? "0" : context.economy().format(data.totalMoney());
        }
        if ("total_actions".equals(key)) {
            return data == null ? "0" : String.valueOf(data.totalActions());
        }
        if ("autosell".equals(key)) {
            return data != null && data.autoSell() ? "вкл" : "выкл";
        }
        if ("profession".equals(key)) {
            return data == null ? "—" : data.profession()
                    .map(profession -> context.config().profession(profession).display())
                    .orElse("—");
        }
        if ("level".equals(key)) {
            return String.valueOf(selectedLevel(data));
        }
        if ("actions".equals(key)) {
            return String.valueOf(selectedStats(data, true));
        }
        if ("money".equals(key)) {
            return data == null ? "0" : context.economy().format(selectedStats(data, false));
        }
        if ("xp".equals(key)) {
            return String.valueOf(selectedXp(data));
        }
        if ("xp_next".equals(key)) {
            return String.valueOf(xpToNext(data));
        }
        if ("progress".equals(key)) {
            return String.valueOf(progress(data));
        }
        if (key.startsWith("boost_")) {
            return boostPlaceholder(data, key.substring("boost_".length()));
        }
        if (key.startsWith("top_")) {
            return topPlaceholder(key.substring("top_".length()));
        }
        if (key.endsWith("_level") || key.endsWith("_xp") || key.endsWith("_money")
                || key.endsWith("_actions") || key.endsWith("_progress")) {
            return perProfession(data, key, offlinePlayer);
        }
        return null;
    }

    private String perProfession(PlayerData data, String key, OfflinePlayer offlinePlayer) {
        int separator = key.lastIndexOf('_');
        if (separator <= 0) {
            return null;
        }
        var profession = Profession.byId(key.substring(0, separator));
        if (profession.isEmpty()) {
            return null;
        }
        Profession target = profession.get();
        String metric = key.substring(separator + 1);
        PlayerData resolved = data;
        if (resolved == null && offlinePlayer.isOnline() && offlinePlayer.getPlayer() != null) {
            resolved = context.players().get(offlinePlayer.getPlayer());
        }
        if (resolved == null) {
            return metric.equals("level") ? "1" : "0";
        }
        var stats = resolved.stats(target);
        return switch (metric) {
            case "level" -> String.valueOf(stats.level());
            case "xp" -> String.valueOf(stats.xp());
            case "actions" -> String.valueOf(stats.actions());
            case "money" -> context.economy().format(stats.money());
            case "progress" -> String.valueOf(context.config().levels()
                    .progressPercent(stats.xp(), stats.level()));
            default -> null;
        };
    }

    private String boostPlaceholder(PlayerData data, String key) {
        if (data == null) {
            return key.endsWith("_time") ? "—" : "1.0";
        }
        switch (key) {
            case "money" -> {
                return Numbers.fixed(context.boosts()
                        .multiplier(data, com.tradewind.model.BoostType.MONEY), 2);
            }
            case "xp" -> {
                return Numbers.fixed(context.boosts()
                        .multiplier(data, com.tradewind.model.BoostType.XP), 2);
            }
            case "money_time" -> {
                return context.boosts().active(data, com.tradewind.model.BoostType.MONEY)
                        .map(boost -> TimeUtil.format(boost.remainingMillis(System.currentTimeMillis())))
                        .orElse("—");
            }
            case "xp_time" -> {
                return context.boosts().active(data, com.tradewind.model.BoostType.XP)
                        .map(boost -> TimeUtil.format(boost.remainingMillis(System.currentTimeMillis())))
                        .orElse("—");
            }
            default -> {
                return null;
            }
        }
    }

    private String topPlaceholder(String key) {
        // top_<profession>_<metric>_<rank>
        String[] parts = key.split("_");
        if (parts.length < 3) {
            return null;
        }
        var profession = Profession.byId(parts[0]);
        int rank = Numbers.parsePositive(parts[parts.length - 1], -1);
        if (profession.isEmpty() || rank < 0) {
            return null;
        }
        LeaderboardEntry entry = context.leaderboards().at(profession.get(), rank);
        if (entry == null) {
            return "-";
        }
        return switch (parts[1]) {
            case "name" -> entry.name();
            case "actions" -> String.valueOf(entry.actions());
            case "money" -> context.economy().format(entry.money());
            case "level" -> String.valueOf(entry.level());
            default -> null;
        };
    }

    private String display(PlayerData data) {
        if (data == null) {
            return "—";
        }
        return data.profession().map(profession -> {
            ProfessionSettings settings = context.config().profession(profession);
            return settings == null ? profession.id() : settings.display();
        }).orElse("—");
    }

    private int selectedLevel(PlayerData data) {
        return data == null ? 1
                : data.profession().map(profession -> data.stats(profession).level()).orElse(1);
    }

    private long selectedStats(PlayerData data, boolean actions) {
        if (data == null || data.profession().isEmpty()) {
            return 0L;
        }
        var stats = data.stats(data.profession().get());
        return actions ? stats.actions() : (long) stats.money();
    }

    private long selectedXp(PlayerData data) {
        return data == null ? 0L
                : data.profession().map(profession -> data.stats(profession).xp()).orElse(0L);
    }

    private long xpToNext(PlayerData data) {
        if (data == null || data.profession().isEmpty()) {
            return 0L;
        }
        return context.config().levels().xpToNextLevel(selectedLevel(data));
    }

    private int progress(PlayerData data) {
        if (data == null || data.profession().isEmpty()) {
            return 0;
        }
        var stats = data.stats(data.profession().get());
        return context.config().levels().progressPercent(stats.xp(), stats.level());
    }

    public static String plain(String value) {
        return Text.plain(Text.render(value));
    }
}

