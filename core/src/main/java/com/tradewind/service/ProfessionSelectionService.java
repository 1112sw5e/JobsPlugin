package com.tradewind.service;

import com.tradewind.bootstrap.PluginContext;
import com.tradewind.config.ProfessionSettings;
import com.tradewind.model.PlayerData;
import com.tradewind.model.Profession;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

/** Choosing (and changing) the active profession, with an in-memory change cooldown. */
public final class ProfessionSelectionService {

    public enum Result {
        SELECTED, SAME, DISABLED, NOT_LOADED, CHANGE_FORBIDDEN, COOLDOWN
    }

    public record Selection(Result result, long remainingMillis, ProfessionSettings settings) {
    }

    private final PluginContext context;
    private final Map<UUID, Long> lastChange = new ConcurrentHashMap<>();

    public ProfessionSelectionService(PluginContext context) {
        this.context = context;
    }

    public Selection select(Player player, Profession profession) {
        ProfessionSettings settings = context.config().profession(profession);
        if (settings == null || !settings.enabled()) {
            return new Selection(Result.DISABLED, 0L, settings);
        }
        PlayerData data = context.players().get(player);
        if (data == null) {
            return new Selection(Result.NOT_LOADED, 0L, settings);
        }
        if (data.profession().orElse(null) == profession) {
            return new Selection(Result.SAME, 0L, settings);
        }
        if (data.profession().isPresent()) {
            if (!context.config().allowProfessionChange()) {
                return new Selection(Result.CHANGE_FORBIDDEN, 0L, settings);
            }
            long cooldown = context.config().professionChangeCooldownMillis();
            Long previous = lastChange.get(player.getUniqueId());
            if (previous != null && cooldown > 0L) {
                long remaining = previous + cooldown - System.currentTimeMillis();
                if (remaining > 0L) {
                    return new Selection(Result.COOLDOWN, remaining, settings);
                }
            }
        }
        data.profession(profession);
        lastChange.put(player.getUniqueId(), System.currentTimeMillis());
        return new Selection(Result.SELECTED, 0L, settings);
    }

    public void clear(UUID uuid) {
        lastChange.remove(uuid);
    }
}

