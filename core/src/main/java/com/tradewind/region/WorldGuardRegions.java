package com.tradewind.region;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;

/**
 * WorldGuard-backed lookup. Loaded only when WorldGuard was found, so servers without it
 * never link this class.
 */
public final class WorldGuardRegions implements RegionService {

    private final Logger logger;
    private final RegionQuery query;

    private WorldGuardRegions(RegionQuery query, Logger logger) {
        this.query = query;
        this.logger = logger;
    }

    public static RegionService create(Logger logger) {
        try {
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            logger.info("WorldGuard подключён: регионы профессий активны.");
            return new WorldGuardRegions(query, logger);
        } catch (LinkageError | RuntimeException failure) {
            logger.log(Level.SEVERE, "WorldGuard найден, но его API не загрузился; регионы отключены.", failure);
            return DisabledRegions.INSTANCE;
        }
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public Set<String> regionIdsAt(Location location) {
        Set<String> ids = new LinkedHashSet<>(2);
        if (location == null || location.getWorld() == null) {
            return ids;
        }
        try {
            ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(location));
            for (ProtectedRegion region : set.getRegions()) {
                ids.add(region.getId().toLowerCase(Locale.ROOT));
            }
        } catch (RuntimeException | LinkageError failure) {
            logger.log(Level.FINE, "Не удалось прочитать регионы в " + location, failure);
        }
        return ids;
    }
}

