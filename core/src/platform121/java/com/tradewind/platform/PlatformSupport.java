package com.tradewind.platform;

import org.bukkit.Bukkit;

/** 1.21.x implementation shipped in the {@code :core} artifact. */
public final class PlatformSupport implements Platform {

    @Override
    public ServerLine expectedLine() {
        return ServerLine.PAPER_121;
    }

    @Override
    public ServerLine runningLine() {
        return Platforms.parse(minecraftVersion());
    }

    @Override
    public String minecraftVersion() {
        return Bukkit.getMinecraftVersion();
    }
}

