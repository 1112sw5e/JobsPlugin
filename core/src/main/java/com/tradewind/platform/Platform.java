package com.tradewind.platform;

/**
 * The single version-specific seam. Each artifact ships its own {@code PlatformSupport},
 * while everything else is shared source.
 */
public interface Platform {

    /** Line this artifact was compiled against. */
    ServerLine expectedLine();

    /** Line the running server reports. */
    ServerLine runningLine();

    String minecraftVersion();

    default String artifactLabel() {
        return expectedLine().artifactLabel();
    }

    default boolean matches() {
        return runningLine() == expectedLine();
    }
}

