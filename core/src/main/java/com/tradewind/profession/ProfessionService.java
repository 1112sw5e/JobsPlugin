package com.tradewind.profession;

import com.tradewind.bootstrap.PluginContext;
import com.tradewind.config.LevelCurve;
import com.tradewind.config.ProfessionSettings;
import com.tradewind.model.BoostType;
import com.tradewind.model.Profession;
import com.tradewind.model.ProfessionStats;
import com.tradewind.model.PlayerData;
import com.tradewind.util.Numbers;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Turns a game action into statistics, salary and experience.
 *
 * <p>Every public entry point validates in the same order: profile loaded, not bypassing,
 * correct gamemode, correct region and profession, correct block, not a player-placed block,
 * rate limit. Only then is anything paid out.
 */
public final class ProfessionService {

    private static final long ACTION_BAR_INTERVAL_MILLIS = 800L;

    private final PluginContext context;
    private final Map<UUID, Long> lastActionBar = new ConcurrentHashMap<>();

    public ProfessionService(PluginContext context) {
        this.context = context;
    }

    public BlockOutcome handleBlockBreak(Player player, Block block) {
        PlayerData data = context.players().get(player);
        if (data == null) {
            return BlockOutcome.NONE;
        }
        ProfessionSettings settings = resolveProfession(player, data, block.getLocation());
        if (settings == null || !settings.accepts(block.getType())) {
            return BlockOutcome.NONE;
        }
        if (settings.profession() == Profession.FARM && !isMature(block)) {
            return BlockOutcome.NONE;
        }
        if (context.config().antiExploit().trackPlacedBlocks()
                && settings.profession() != Profession.FARM
                && context.placedBlocks().isTracked(block)) {
            context.debug("пропуск: блок поставлен игроком " + block.getType());
            return BlockOutcome.NONE;
        }
        if (!context.limiter().allow(player.getUniqueId())) {
            return BlockOutcome.NONE;
        }
        context.placedBlocks().forget(block);

        ItemStack tool = player.getInventory().getItemInMainHand();
        Collection<ItemStack> drops;
        try {
            drops = block.getDrops(tool, player);
        } catch (RuntimeException failure) {
            drops = List.of();
        }
        return pay(player, data, settings, drops, true);
    }

    /**
     * @param caught the caught item, or {@code null} for a failed catch
     */
    public BlockOutcome handleFish(Player player, ItemStack caught) {
        PlayerData data = context.players().get(player);
        if (data == null || caught == null) {
            return BlockOutcome.NONE;
        }
        ProfessionSettings settings = resolveProfession(player, data, player.getLocation());
        if (settings == null || settings.profession() != Profession.FISH) {
            return BlockOutcome.NONE;
        }
        if (!context.limiter().allow(player.getUniqueId())) {
            return BlockOutcome.NONE;
        }
        return pay(player, data, settings, List.of(caught), true);
    }

    private BlockOutcome pay(Player player, PlayerData data, ProfessionSettings settings,
                             Collection<ItemStack> drops, boolean allowAutoSell) {
        LevelCurve curve = context.config().levels();
        ProfessionStats stats = data.stats(settings.profession());
        int levelBefore = stats.level();
        double levelMultiplier = curve.salaryMultiplier(levelBefore);
        double moneyBoost = context.boosts().multiplier(data, BoostType.MONEY);
        double xpBoost = context.boosts().multiplier(data, BoostType.XP);

        double earned = settings.salaryPerAction() * levelMultiplier * moneyBoost;
        List<ItemStack> unsold = new ArrayList<>();
        boolean sold = false;

        if (allowAutoSell && data.autoSell() && context.config().buyer().enabled()) {
            for (ItemStack drop : drops) {
                double price = autoSellPrice(drop, levelMultiplier, moneyBoost);
                if (price > 0.0D) {
                    earned += price;
                    sold = true;
                } else if (drop != null && !drop.getType().isAir()) {
                    unsold.add(drop);
                }
            }
        } else {
            unsold.addAll(drops);
        }

        earned = Numbers.round(earned, context.config().economyRoundTo());
        long xp = Math.max(0L, Math.round(settings.xpPerAction() * xpBoost));

        stats.addAction();
        stats.addXp(xp);
        if (earned > 0.0D) {
            stats.addMoney(earned);
            context.economy().deposit(player, earned);
        }
        data.markDirty();

        applyLevel(curve, player, settings, data, stats, levelBefore);
        showActionBar(player, settings, stats, earned);
        return new BlockOutcome(new Reward(true, earned, xp), sold, List.copyOf(unsold));
    }

    private void applyLevel(LevelCurve curve, Player player, ProfessionSettings settings,
                            PlayerData data, ProfessionStats stats, int levelBefore) {
        if (!curve.enabled()) {
            return;
        }
        int levelAfter = curve.levelForXp(stats.xp());
        if (levelAfter <= levelBefore) {
            return;
        }
        stats.level(levelAfter);
        data.markDirty();
        double reward = curve.reward(levelAfter);
        if (reward > 0.0D) {
            context.economy().deposit(player, reward);
            stats.addMoney(reward);
        }
        context.messages().send(player, "profession.level-up",
                "profession", settings.display(),
                "level", String.valueOf(levelAfter),
                "reward", context.economy().format(reward));
    }

    private void showActionBar(Player player, ProfessionSettings settings,
                               ProfessionStats stats, double earned) {
        long now = System.currentTimeMillis();
        Long last = lastActionBar.get(player.getUniqueId());
        if (last != null && now - last < ACTION_BAR_INTERVAL_MILLIS) {
            return;
        }
        lastActionBar.put(player.getUniqueId(), now);
        player.sendActionBar(context.messages().component("profession.action-bar",
                Map.of("profession", settings.display(),
                        "money", context.economy().format(earned),
                        "xp", String.valueOf(stats.xp()),
                        "level", String.valueOf(stats.level()))));
    }

    /** Price of one drop after level and money multipliers, 0 when not sellable. */
    private double autoSellPrice(ItemStack drop, double levelMultiplier, double moneyBoost) {
        if (drop == null || drop.getType().isAir() || context.items().isCustom(drop)) {
            return 0.0D;
        }
        double unit = context.config().prices().price(drop.getType());
        if (unit <= 0.0D) {
            return 0.0D;
        }
        return unit * drop.getAmount() * levelMultiplier * moneyBoost;
    }

    private ProfessionSettings resolveProfession(Player player, PlayerData data, Location location) {
        if (player.hasPermission(context.config().antiExploit().bypassPermission())
                || (context.config().antiExploit().denyCreative()
                && player.getGameMode() == GameMode.CREATIVE)
                || player.getGameMode() == GameMode.SPECTATOR) {
            return null;
        }
        var antiExploit = context.config().antiExploit();
        Profession regionProfession = null;
        if (context.regions().available()) {
            for (String id : context.regions().regionIdsAt(location)) {
                Profession mapped = context.config().professionByRegion(id);
                if (mapped != null) {
                    regionProfession = mapped;
                    break;
                }
            }
        }
        Profession selected = data.profession().orElse(null);
        Profession effective;
        if (regionProfession != null) {
            if (selected != regionProfession) {
                return null;
            }
            effective = regionProfession;
        } else {
            if (antiExploit.requireRegion() || selected == null) {
                return null;
            }
            effective = selected;
        }
        ProfessionSettings settings = context.config().profession(effective);
        return settings != null && settings.enabled() ? settings : null;
    }

    private static boolean isMature(Block block) {
        if (block.getBlockData() instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        return true;
    }

    public void clear(UUID uuid) {
        lastActionBar.remove(uuid);
    }

    /** Convenience for diagnostics. */
    public String describe(Location location) {
        return context.regions().regionIdsAt(location).stream()
                .map(id -> id + "=" + (context.config().professionByRegion(id) == null
                        ? "-" : context.config().professionByRegion(id).id().toLowerCase(Locale.ROOT)))
                .reduce((a, b) -> a + ", " + b)
                .orElse("нет региона");
    }
}

