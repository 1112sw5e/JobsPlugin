package com.tradewind.region;

import java.util.Set;
import org.bukkit.Location;

/** Used when WorldGuard is absent: no region, therefore no profession actions. */
public final class DisabledRegions implements RegionService {

    public static final DisabledRegions INSTANCE = new DisabledRegions();

    private DisabledRegions() {
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public Set<String> regionIdsAt(Location location) {
        return Set.of();
    }
}

