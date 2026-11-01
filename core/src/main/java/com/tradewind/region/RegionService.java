package com.tradewind.region;

import java.util.Set;
import org.bukkit.Location;

/**
 * Region lookup seam. The implementation links against WorldGuard; {@link #available()}
 * lets callers skip it entirely when WorldGuard is missing.
 */
public interface RegionService {

    boolean available();

    /** Lowercase ids of every region containing the location. */
    Set<String> regionIdsAt(Location location);
}

