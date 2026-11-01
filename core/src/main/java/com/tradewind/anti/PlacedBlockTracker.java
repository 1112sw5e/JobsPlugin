package com.tradewind.anti;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.block.Block;

/**
 * Remembers blocks placed by players so place-and-break cannot pay out.
 *
 * <p>Crops are deliberately not tracked: a farmer plants and then harvests, and maturity is
 * checked instead. Entries expire, so a long-lived world does not leak.
 */
public final class PlacedBlockTracker {

    public record BlockKey(String world, int x, int y, int z) {
    }

    private final Map<BlockKey, Long> placed = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public PlacedBlockTracker(long ttlMillis) {
        this.ttlMillis = Math.max(1000L, ttlMillis);
    }

    public void record(Block block) {
        if (block == null || block.getWorld() == null) {
            return;
        }
        placed.put(key(block), System.currentTimeMillis());
    }

    public boolean isTracked(Block block) {
        if (block == null || block.getWorld() == null) {
            return false;
        }
        Long timestamp = placed.get(key(block));
        if (timestamp == null) {
            return false;
        }
        if (System.currentTimeMillis() - timestamp > ttlMillis) {
            placed.remove(key(block));
            return false;
        }
        return true;
    }

    public void forget(Block block) {
        if (block != null && block.getWorld() != null) {
            placed.remove(key(block));
        }
    }

    public void prune() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<BlockKey, Long>> iterator = placed.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue() > ttlMillis) {
                iterator.remove();
            }
        }
    }

    public int size() {
        return placed.size();
    }

    private static BlockKey key(Block block) {
        return new BlockKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }
}

