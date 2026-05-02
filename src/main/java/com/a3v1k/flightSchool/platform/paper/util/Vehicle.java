package com.a3v1k.flightSchool.platform.paper.util;

import com.a3v1k.flightSchool.domain.player.Role;
import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import com.a3v1k.flightSchool.domain.match.GameState;
import com.a3v1k.flightSchool.domain.player.GamePlayer;
import com.a3v1k.flightSchool.domain.team.Team;
import com.a3v1k.flightSchool.platform.paper.game.PaperGameManager;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@UtilityClass
public class Vehicle {

    private static final int RESPAWN_COUNTDOWN_SECONDS = 5;

    public void explodePlane(Entity entity, Entity damager) {
        if (entity == null) return;

        FlightSchool plugin = FlightSchool.getInstance();
        if (plugin == null) return;

        MythicBukkit mythic = MythicBukkit.inst();
        if (!mythic.getMobManager().isMythicMob(entity)) return;

        Optional<ActiveMob> victimOpt = mythic.getMobManager().getActiveMob(entity.getUniqueId());
        if (victimOpt.isEmpty()) return;

        ActiveMob victim = victimOpt.get();
        String teamName = extractTeamName(victim);
        if (teamName == null) return;

        Team team = plugin.getGameManager().getTeam(teamName);
        if (team == null) return;

        if (isPlane(victim)) {
            handlePlaneDeath(plugin, entity, victim, team, teamName);
            return;
        }

        if (isTurret(victim)) {
            if (damager == null) return;
            if (!mythic.getMobManager().isMythicMob(damager)) return;

            Optional<ActiveMob> attackerOpt = mythic.getMobManager().getActiveMob(damager.getUniqueId());
            if (attackerOpt.isEmpty()) return;

            handleTurretDeath(plugin, entity, victim, team, teamName);
        }
    }

    /* Plane Death */

    private static void handlePlaneDeath(
        FlightSchool plugin,
        Entity entity,
        ActiveMob victim,
        Team team,
        String teamName
    ) {
        Player pilot = getOwnerPlayer(entity);
        if (pilot == null) return;

        List<Location> planeLocations = plugin.getConfigManager().getPlaneLocations().get(teamName);
        if (planeLocations == null || planeLocations.isEmpty()) return;

        Location respawnLocation = resolveRespawnLocation(plugin, team, pilot, planeLocations);

        spawnTeamFirework(entity, team);

        plugin.getKillcamManager().stopRecording(pilot);
        pilot.setGameMode(GameMode.SPECTATOR);

        spawnRespawnRunnable(plugin, pilot, team, teamName, respawnLocation);
    }

    private Location resolveRespawnLocation(
        FlightSchool plugin,
        Team team,
        Player pilot,
        List<Location> planeLocations
    ) {
        int teamIndex = plugin.getGameManager().getPlaneMembers(team).indexOf(pilot);
        int respawnIndex = (teamIndex >= 0)
            ? Math.min(teamIndex, planeLocations.size() - 1)
            : 0;
        return planeLocations.get(respawnIndex).clone();
    }

    private void spawnRespawnRunnable(
        FlightSchool plugin,
        Player pilot,
        Team team,
        String teamName,
        Location respawnLocation
    ) {
        // TODO
    }

    private static void handleRespawnAttempt(
        FlightSchool plugin,
        Player pilot,
        Team team,
        String teamName,
        Location respawnLocation
    ) {
        // TODO
    }

    /* Turret Death */

    private static void handleTurretDeath(
        FlightSchool plugin,
        Entity entity,
        ActiveMob victim,
        Team team,
        String teamName
    ) {
        Player operator = getOwnerPlayer(entity);
        if (operator == null) return;

        operator.setGameMode(GameMode.SPECTATOR);
        operator.showTitle(Title.title(
            Component.text("Cannon Destroyed!", TextColor.color(255, 0, 0)),
            Component.text("You are now a spectator."),
            Title.Times.times(
                Duration.ofMillis(500),
                Duration.ofMillis(3500),
                Duration.ofMillis(1000)
            )
        ));

        team.increaseDestroyedBlimps();

        notifyTeamCannonLost(plugin, team);

        if (team.getDestroyedBlimps() >= plugin.getGameManager()
            .getRoleLimit(Role.CANNON_OPERATOR)) {
            eliminateTeam(plugin, team, teamName);
        }
    }

    private static void notifyTeamCannonLost(FlightSchool plugin, Team team) {
        int totalCannons = plugin.getGameManager().getRoleLimit(com.a3v1k.flightSchool.domain.player.Role.CANNON_OPERATOR);

        Component message = Component.text("A cannon has been destroyed!", NamedTextColor.RED)
            .append(Component.newline())
            .append(Component.text(
                team.getDestroyedBlimps() + "/" + totalCannons + " cannons lost.",
                NamedTextColor.GOLD
            ));

        broadcastToTeam(team, message);
    }

    private static void eliminateTeam(FlightSchool plugin, Team team, String teamName) {
        team.setBlimpDestroyed(true);

        Component eliminatedMessage = Component.text("════════════════════════════════", NamedTextColor.DARK_RED)
            .append(Component.newline())
            .append(Component.text("Your blimp has been destroyed!", NamedTextColor.RED))
            .append(Component.newline())
            .append(Component.newline())
            .append(Component.text("Your team has been eliminated!", NamedTextColor.GOLD))
            .append(Component.newline())
            .append(Component.text("════════════════════════════════", NamedTextColor.DARK_RED));

        broadcastToTeam(team, eliminatedMessage);
        setTeamSpectator(team);
        despawnTeamPlanes(plugin, team);

        plugin.getGameOrchestrator().explodeBlimp(teamName);

        // Last-team-standing check: if at most one team still has alive turrets,
        // end the match. triggerMatchEnd no-ops if state isn't IN_GAME.
        long aliveCount = plugin.getGameManager().getTeams().values().stream()
            .filter(plugin.getGameManager()::teamHasAliveTurret)
            .count();
        if (aliveCount <= 1) {
            plugin.getGameOrchestrator().triggerMatchEnd();
        }
    }

    private static void setTeamSpectator(Team team) {
        for (UUID uuid : team.getMembers()) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null && member.isOnline()) {
                member.setGameMode(GameMode.SPECTATOR);
            }
        }
    }

    private static void despawnTeamPlanes(FlightSchool plugin, Team team) {
        if (!(plugin.getGameManager() instanceof PaperGameManager paperGameManager)) return;

        List<ActiveMob> teamPlanes = paperGameManager.getTeamPlanes().get(team);
        if (teamPlanes == null) return;

        for (ActiveMob plane : teamPlanes) {
            if (plane != null && !plane.isDead()) {
                plane.despawn();
            }
        }
    }

    /* Helpers */

    private Player getOwnerPlayer(Entity entity) {
        String uuidString = entity.getPersistentDataContainer()
            .get(PdcKeys.OWNER_UUID, PersistentDataType.STRING);
        if (uuidString == null) return null;
        try {
            return Bukkit.getPlayer(UUID.fromString(uuidString));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isPlane(ActiveMob mob) {
        String faction = mob.getFaction();
        return faction != null && faction.contains("plane");
    }

    private boolean isTurret(ActiveMob mob) {
        String faction = mob.getFaction();
        return faction != null && faction.contains("turret");
    }

    private String extractTeamName(ActiveMob mob) {
        String beam = mob.getVariables().getString("beam");
        if (beam != null && beam.startsWith("beam_") && beam.length() > "beam_".length()) {
            return beam.substring("beam_".length());
        }

        String faction = mob.getFaction();
        if (faction == null) return null;

        if (faction.startsWith("plane_")) return faction.substring("plane_".length());
        if (faction.startsWith("turret_")) return faction.substring("turret_".length());

        return null;
    }

    private static void broadcastToTeam(Team team, Component message) {
        for (UUID uuid : team.getMembers()) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null && member.isOnline()) {
                member.sendMessage(message);
            }
        }
    }

    private static void spawnTeamFirework(Entity entity, Team team) {
        Firework firework = entity.getWorld().spawn(entity.getLocation(), Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
            .withColor(team.getColor())
            .with(FireworkEffect.Type.BALL)
            .build());
        firework.setFireworkMeta(meta);
        firework.detonate();
    }

}
