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

        // Mark eliminated immediately so checkLastTeamStanding (called below) sees an
        // accurate state. If the respawn is allowed, the runLater inside
        // PaperGameManager.spawnDelayedPlane will reset eliminated=false before
        // the plane is spawned again.
        GamePlayer pilotGp = plugin.getGameManager().getGamePlayer(pilot.getUniqueId());
        if (pilotGp != null) pilotGp.setEliminated(true);

        List<Location> planeLocations = plugin.getConfigManager().getPlaneLocations().get(teamName);
        if (planeLocations == null || planeLocations.isEmpty()) return;

        Location respawnLocation = resolveRespawnLocation(plugin, team, pilot, planeLocations);

        spawnTeamFirework(entity, team);

        plugin.getKillcamManager().stopRecording(pilot);
        pilot.setGameMode(GameMode.SPECTATOR);

        spawnRespawnRunnable(plugin, pilot, team, teamName, respawnLocation);

        // Re-check after every plane death: if another team was already eliminated
        // and this death (or its blocked respawn) leaves only one team with active
        // players, the match should end. checkLastTeamStanding is idempotent.
        plugin.getGameOrchestrator().checkLastTeamStanding();
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

    /**
     * @deprecated Dead code — never called. The respawn flow runs through
     * {@code KillcamManager.onReplayFinished} → {@code GameOrchestrator.spawnDelayedPlane}.
     * Slated for removal in a future cleanup pass.
     */
    @Deprecated
    private void spawnRespawnRunnable(
        FlightSchool plugin,
        Player pilot,
        Team team,
        String teamName,
        Location respawnLocation
    ) {
        // TODO: dead code, see deprecation notice
    }

    /**
     * @deprecated Dead code — never called. The actual respawn-block logic lives in
     * {@code PaperGameManager.spawnDelayedPlane}. Slated for removal in a future
     * cleanup pass.
     */
    @Deprecated
    private static void handleRespawnAttempt(
        FlightSchool plugin,
        Player pilot,
        Team team,
        String teamName,
        Location respawnLocation
    ) {
        // TODO: dead code, see deprecation notice
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

        // Cannon operators have no respawn — mark this player out for the rest of the round.
        GamePlayer operatorGp = plugin.getGameManager().getGamePlayer(operator.getUniqueId());
        if (operatorGp != null) operatorGp.setEliminated(true);

        team.increaseDestroyedBlimps();

        notifyTeamCannonLost(plugin, team);

        if (team.getDestroyedBlimps() >= plugin.getGameManager()
            .getRoleLimit(Role.CANNON_OPERATOR)) {
            eliminateTeam(plugin, team, teamName);
        } else {
            // Defensive — another team may already be down to its last cannon player.
            plugin.getGameOrchestrator().checkLastTeamStanding();
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

        // Mark every team member eliminated so checkLastTeamStanding sees this team
        // as out. This covers cannon operators already in spectator AND any plane
        // pilots whose planes are about to be despawned.
        for (UUID memberId : team.getMembers()) {
            GamePlayer gp = plugin.getGameManager().getGamePlayer(memberId);
            if (gp != null) gp.setEliminated(true);
        }

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

        // Single source of truth for end-of-match detection: counts teams with any
        // non-eliminated player and triggers match end if only one (or zero) remain.
        plugin.getGameOrchestrator().checkLastTeamStanding();

        // Turret-based fallback: covers cases where the player-eliminated bookkeeping
        // misses a team (e.g. a player rejoined after team elimination got a fresh
        // GamePlayer with eliminated=false). triggerMatchEnd is idempotent and no-ops
        // outside IN_GAME.
        long teamsWithAliveTurrets = plugin.getGameManager().getTeams().values().stream()
            .filter(plugin.getGameManager()::teamHasAliveTurret)
            .count();
        if (teamsWithAliveTurrets <= 1) {
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
