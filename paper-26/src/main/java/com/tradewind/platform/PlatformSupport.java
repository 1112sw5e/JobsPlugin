package com.tradewind.platform;

import org.bukkit.Bukkit;

/** 26.1.x implementation shipped in the {@code :paper-26} artifact. */
public final class PlatformSupport implements Platform {

    @Override
    public ServerLine expectedLine() {
        return ServerLine.PAPER_26;
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

