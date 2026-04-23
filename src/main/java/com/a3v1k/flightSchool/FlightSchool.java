package com.a3v1k.flightSchool;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Compatibility entrypoint for configs and integrations that still reference
 * the original plugin main class.
 */
public final class FlightSchool extends com.a3v1k.flightSchool.platform.paper.FlightSchool {

    public static FlightSchool getInstance() {
        return JavaPlugin.getPlugin(FlightSchool.class);
    }
}
