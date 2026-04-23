package com.a3v1k.flightSchool.util;

import org.bukkit.entity.Entity;

/**
 * Compatibility facade for external configs that reference the old utility
 * class name.
 */
public final class Vehicle {

    private Vehicle() {
    }

    public static void explodePlane(Entity entity, Entity damager) {
        com.a3v1k.flightSchool.platform.paper.util.Vehicle.explodePlane(entity, damager);
    }
}
