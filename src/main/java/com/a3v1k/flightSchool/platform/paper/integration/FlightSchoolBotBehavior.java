package com.a3v1k.flightSchool.platform.paper.integration;

import com.a3v1k.flightSchool.application.game.GameManager;
import com.a3v1k.flightSchool.domain.blimp.BlimpData;
import com.a3v1k.flightSchool.domain.match.GameState;
import com.a3v1k.flightSchool.domain.team.Team;
import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import creatorsplash.creatorsplashcore.bots.Bot;
import creatorsplash.creatorsplashcore.bots.BotBehavior;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

final class FlightSchoolBotBehavior implements BotBehavior {

    private static final double ENGAGE_RANGE = 90.0;
    private static final double ORBIT_RADIUS = 22.0;
    private static final int SHOT_INTERVAL_TICKS = 40;

    private final FlightSchool plugin;
    private final @Nullable String fsTeam;

    private double orbitAngle;
    private long nextShotAge;

    FlightSchoolBotBehavior(FlightSchool plugin, @Nullable String fsTeam) {
        this.plugin = plugin;
        this.fsTeam = fsTeam;
    }

    @Override
    public void tick(Bot bot, long age) {
        GameManager gm = plugin.getGameManager();
        if (gm.getGameState() != GameState.IN_GAME) {
            return;
        }
        Player self = bot.bukkit();
        if (self.isDead()) return;

        Entity vehicle = self.getVehicle();
        if (vehicle == null || !vehicle.isValid()) {
            return;
        }

        BlimpData targetBlimp = pickTargetBlimp(gm, self);
        if (targetBlimp == null) return;
        Location center = targetBlimp.getCenter();
        if (center.getWorld() == null || !center.getWorld().equals(self.getWorld())) return;

        orbitAngle += 0.02;
        double distSq = self.getLocation().distanceSquared(center);
        Location lookPoint;
        if (distSq > 55.0 * 55.0) {
            lookPoint = center;
        } else {
            lookPoint = center.clone().add(
                    Math.cos(orbitAngle) * ORBIT_RADIUS, 8.0, Math.sin(orbitAngle) * ORBIT_RADIUS);
        }
        bot.lookAt(lookPoint);

        if (fsTeam != null && age >= nextShotAge && distSq <= ENGAGE_RANGE * ENGAGE_RANGE) {
            bot.lookAt(center);
            if (MythicBukkit.inst().getMobManager().isMythicMob(vehicle)) {
                try {
                    MythicBukkit.inst().getAPIHelper().castSkill(vehicle, "plane_" + fsTeam + "_shot");
                    nextShotAge = age + SHOT_INTERVAL_TICKS;
                } catch (Throwable ignored) {
                    nextShotAge = age + SHOT_INTERVAL_TICKS * 4L;
                }
            }
        }
    }

    private @Nullable BlimpData pickTargetBlimp(GameManager gm, Player self) {
        BlimpData nearest = null;
        double nearestSq = Double.MAX_VALUE;
        for (Team team : gm.getTeams().values()) {
            if (team == null || team.getBlimpDestroyed()) continue;
            if (fsTeam != null && fsTeam.equalsIgnoreCase(team.getName())) continue;
            BlimpData blimp = gm.getBlimp(team.getName());
            if (blimp == null || blimp.getCenter() == null) continue;
            if (blimp.getCenter().getWorld() == null
                    || !blimp.getCenter().getWorld().equals(self.getWorld())) continue;
            double distSq = blimp.getCenter().distanceSquared(self.getLocation());
            if (distSq < nearestSq) {
                nearestSq = distSq;
                nearest = blimp;
            }
        }
        return nearest;
    }
}
