package com.a3v1k.flightSchool.util;

import com.a3v1k.flightSchool.FlightSchool;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.skills.variables.Variable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;

import java.io.IOException;
import java.util.*;

public class ConfigManager {

    private final FileManager fileManager;

    public ConfigManager() {
        FlightSchool plugin = FlightSchool.getInstance();
        this.fileManager = new FileManager("config.yml", plugin);

        try {
            this.fileManager.createFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addCannonLocation(String teamName, Location location) {
        YamlConfiguration config = this.fileManager.getConfig();

        config.set("cannons." + teamName + "." + UUID.randomUUID().toString(), location);

        try {
            this.fileManager.save(config);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addPlaneLocation(String teamName, Location location) {
        YamlConfiguration config = this.fileManager.getConfig();

        config.set("planes." + teamName + "." + UUID.randomUUID().toString(), location);

        try {
            this.fileManager.save(config);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, List<Location>> getCannonLocations() {
        YamlConfiguration config = this.fileManager.getConfig();
        Map<String, List<Location>> locationMap = new HashMap<>();

        ConfigurationSection cannons = config.getConfigurationSection("cannons");
        if (cannons == null) return locationMap;

        for (String teamName : cannons.getKeys(false)) {
            List<Location> locations = new ArrayList<>();

            ConfigurationSection teamSection = cannons.getConfigurationSection(teamName);
            if (teamSection == null) continue;

            for (String uuid : teamSection.getKeys(false)) {
                Location loc = teamSection.getLocation(uuid);
                if (loc != null) {
                    locations.add(loc);
                }
            }

            locationMap.put(teamName, locations);
        }

        return locationMap;
    }

    public Map<String, List<Location>> getPlaneLocations() {
        YamlConfiguration config = this.fileManager.getConfig();
        Map<String, List<Location>> locationMap = new HashMap<>();

        ConfigurationSection planes = config.getConfigurationSection("planes");
        if (planes == null) return locationMap;

        for (String teamName : planes.getKeys(false)) {
            List<Location> locations = new ArrayList<>();

            ConfigurationSection teamSection = planes.getConfigurationSection(teamName);
            if (teamSection == null) continue;

            for (String uuid : teamSection.getKeys(false)) {
                Location loc = teamSection.getLocation(uuid);
                if (loc != null) {
                    locations.add(loc);
                }
            }

            locationMap.put(teamName, locations);
        }

        return locationMap;
    }

    public int getMinFlightY(World world, Map<String, List<Location>> planeLocations) {
        YamlConfiguration config = this.fileManager.getConfig();
        int fallbackFloorY = resolveDefaultPlaneFloorY(world, planeLocations);

        if (!config.contains("airspace.min-flight-y")) {
            config.set("airspace.min-flight-y", fallbackFloorY);

            try {
                this.fileManager.save(config);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return Math.max(world.getMinHeight() + 1, config.getInt("airspace.min-flight-y", fallbackFloorY));
    }

    public int getMaxFlightY(World world, Map<String, List<Location>> planeLocations) {
        YamlConfiguration config = this.fileManager.getConfig();
        int fallbackCeilingY = resolveDefaultPlaneCeilingY(world, planeLocations);

        if (!config.contains("airspace.max-flight-y")) {
            config.set("airspace.max-flight-y", fallbackCeilingY);

            try {
                this.fileManager.save(config);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return Math.min(world.getMaxHeight() - 1, config.getInt("airspace.max-flight-y", fallbackCeilingY));
    }

    private int resolveDefaultPlaneFloorY(World world, Map<String, List<Location>> planeLocations) {
        int lowestPlaneSpawnY = planeLocations.values().stream()
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .mapToInt(Location::getBlockY)
                .min()
                .orElse(world.getSpawnLocation().getBlockY());

        return Math.max(world.getMinHeight() + 1, lowestPlaneSpawnY - 96);
    }

    private int resolveDefaultPlaneCeilingY(World world, Map<String, List<Location>> planeLocations) {
        int highestPlaneSpawnY = planeLocations.values().stream()
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .mapToInt(Location::getBlockY)
                .max()
                .orElse(world.getSpawnLocation().getBlockY());

        return Math.min(world.getMaxHeight() - 1, highestPlaneSpawnY + 160);
    }
}
