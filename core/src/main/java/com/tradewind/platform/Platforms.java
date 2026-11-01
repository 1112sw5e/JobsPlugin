package com.tradewind.platform;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Maps a Minecraft version string onto a {@link ServerLine}. */
public final class Platforms {

    private static final Pattern VERSION = Pattern.compile("^(\\d+)\\.(\\d+)");

    private Platforms() {
    }

    public static ServerLine parse(String minecraftVersion) {
        if (minecraftVersion == null || minecraftVersion.isBlank()) {
            return ServerLine.UNKNOWN;
        }
        Matcher matcher = VERSION.matcher(minecraftVersion.trim());
        if (!matcher.find()) {
            return ServerLine.UNKNOWN;
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        if (major == 1 && minor >= 21) {
            return ServerLine.PAPER_121;
        }
        if (major == 26 && minor == 1) {
            return ServerLine.PAPER_26;
        }
        return ServerLine.UNKNOWN;
    }
}

