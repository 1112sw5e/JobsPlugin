package com.tradewind.bootstrap;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.bukkit.plugin.PluginManager;

/** Snapshot of which external plugins were present on the last load/reload. */
public final class DependencyReport {

    private final EnumSet<Dependency> present;
    private final EnumSet<Dependency> missingRequired;
    private final EnumSet<Dependency> missingOptional;

    private DependencyReport(EnumSet<Dependency> present,
                             EnumSet<Dependency> missingRequired,
                             EnumSet<Dependency> missingOptional) {
        this.present = present;
        this.missingRequired = missingRequired;
        this.missingOptional = missingOptional;
    }

    public static DependencyReport scan(PluginManager pluginManager) {
        EnumSet<Dependency> present = EnumSet.noneOf(Dependency.class);
        EnumSet<Dependency> missingRequired = EnumSet.noneOf(Dependency.class);
        EnumSet<Dependency> missingOptional = EnumSet.noneOf(Dependency.class);
        for (Dependency dependency : Dependency.values()) {
            if (pluginManager.getPlugin(dependency.pluginName()) != null) {
                present.add(dependency);
            } else if (dependency.required()) {
                missingRequired.add(dependency);
            } else {
                missingOptional.add(dependency);
            }
        }
        return new DependencyReport(present, missingRequired, missingOptional);
    }

    public boolean isPresent(Dependency dependency) {
        return present.contains(dependency);
    }

    /** True when every required dependency was found. */
    public boolean satisfied() {
        return missingRequired.isEmpty();
    }

    public Set<Dependency> present() {
        return Collections.unmodifiableSet(present.clone());
    }

    public Set<Dependency> missingRequired() {
        return Collections.unmodifiableSet(missingRequired.clone());
    }

    public Set<Dependency> missingOptional() {
        return Collections.unmodifiableSet(missingOptional.clone());
    }

    public String summary() {
        if (missingRequired.isEmpty() && missingOptional.isEmpty()) {
            return "все на месте";
        }
        StringJoiner joiner = new StringJoiner("; ");
        if (!missingRequired.isEmpty()) {
            joiner.add("нет обязательных: " + names(missingRequired));
        }
        if (!missingOptional.isEmpty()) {
            joiner.add("нет мягких: " + names(missingOptional));
        }
        return joiner.toString();
    }

    public static String names(Collection<Dependency> dependencies) {
        return dependencies.stream().map(Dependency::pluginName).collect(Collectors.joining(", "));
    }
}

