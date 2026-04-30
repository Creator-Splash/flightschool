package com.a3v1k.flightSchool.platform.paper.game.plane;

import com.a3v1k.flightSchool.application.game.GameManager;
import com.a3v1k.flightSchool.application.scheduler.Scheduler;
import com.a3v1k.flightSchool.domain.team.Team;
import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import com.a3v1k.flightSchool.platform.paper.game.blimp.PaperBlimpHealthManager;
import com.a3v1k.flightSchool.platform.paper.util.PdcKeys;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@RequiredArgsConstructor
public final class PlaneSpawnService {

    private static final Component CANNON_MESSAGE = Component
        .text("════════════════════════════════", NamedTextColor.DARK_GREEN)
            .append(Component.newline())
            .append(Component.text("You are a cannon!", NamedTextColor.GREEN))
            .append(Component.newline())
            .append(Component.newline())
            .append(Component.text("Protect your blimp against other planes! Use [SPACE] to shoot!",
                NamedTextColor.GOLD))
            .append(Component.newline())
            .append(Component.text("════════════════════════════════",
                NamedTextColor.DARK_GREEN));

    private static final Component PLANE_MESSAGE = Component
        .text("════════════════════════════════", NamedTextColor.DARK_GREEN)
            .append(Component.newline())
            .append(Component.text("You are a plane!", NamedTextColor.GREEN))
            .append(Component.newline())
            .append(Component.newline())
            .append(Component.text("Shoot down other planes and protect your blimp! Use [SPACE] to shoot!",
                NamedTextColor.GOLD))
            .append(Component.newline())
            .append(Component.text("════════════════════════════════",
                NamedTextColor.DARK_GREEN));

    private final FlightSchool plugin;
    private final GameManager gameManager;
    private final Scheduler scheduler;
    private final Logger logger;
    private final Map<Team, List<ActiveMob>> teamPlaneMaps;
    private final Map<String, PaperBlimpHealthManager> healthManagers;
    private final Map<String, Scheduler.Task> blimpHealthTasks;

    /* Initial Spawn */

    public void spawnCannons(Map<String, List<Location>> cannonLocations) {
        for (Map.Entry<String, List<Location>> entry : cannonLocations.entrySet()) {
            Team team = gameManager.getTeam(entry.getKey());
            if (team == null) {
                logger.warning("No team found for cannon location key: " + entry.getKey());
                continue;
            }

            List<Player> players = gameManager.getCannonMembers(team);
            List<ActiveMob> activeMobs = new ArrayList<>();
            int index = 0;

            for (Location location : entry.getValue()) {
                Optional<ActiveMob> mobOpt = spawnMob(
                        "flightschool_turret_" + entry.getKey().toLowerCase(), location);

                if (mobOpt.isEmpty()) {
                    logger.warning("No turret mob found for team: " + entry.getKey());
                    continue;
                }

                ActiveMob spawnedMob = mobOpt.get();

                if (index >= players.size()) {
                    spawnedMob.despawn();
                    continue;
                }

                Player player = players.get(index);
                activeMobs.add(spawnedMob);

                assignOwner(spawnedMob, player);
                preparePlayer(player, location, CANNON_MESSAGE);
                location.clone().add(2, -3, 2).getBlock().setType(Material.LIGHT);
                scheduleMountSignal(spawnedMob, player, "mountCannon");
                index++;
            }

            registerHealthManager(team, activeMobs);
        }
    }

    public void spawnPlanes(Map<String, List<Location>> planeLocations) {
        for (Map.Entry<String, List<Location>> entry : planeLocations.entrySet()) {
            Team team = gameManager.getTeam(entry.getKey());
            if (team == null) {
                logger.warning("No team found for plane location key: " + entry.getKey());
                continue;
            }

            List<Player> players = gameManager.getPlaneMembers(team);
            teamPlaneMaps.put(team, new ArrayList<>());
            int index = 0;

            for (Location location : entry.getValue()) {
                Optional<ActiveMob> mobOpt = spawnMob("plane_" + team.getName(), location);

                if (mobOpt.isEmpty()) {
                    logger.warning("No plane mob found for team: " + team.getName());
                    continue;
                }

                ActiveMob spawnedMob = mobOpt.get();

                if (index >= players.size()) {
                    spawnedMob.despawn();
                    continue;
                }

                Player player = players.get(index);
                registerActivePlane(team, player, spawnedMob);

                assignOwner(spawnedMob, player);
                preparePlayer(player, location, PLANE_MESSAGE);
                plugin.getKillcamManager().startRecording(player);
                index++;

                scheduleMountSignal(spawnedMob, player, "mountPlane");
                scheduler.runLater(() -> {
                    if (!spawnedMob.getEntity().getBukkitEntity().getPassengers().contains(player)) {
                        spawnedMob.getEntity().getBukkitEntity().addPassenger(player);
                    }
                }, 10L);
            }
        }
    }

    /* Respawn */

    public void spawnPlane(String teamName, Location location, Player player) {
        Optional<ActiveMob> mobOpt = spawnMob("plane_" + teamName.toLowerCase(), location)
            .or(() -> spawnMob("flightschool_plane_" + teamName.toLowerCase(), location));

        if (mobOpt.isEmpty()) {
            logger.warning("No plane mob found for team: " + teamName);
            return;
        }

        ActiveMob spawnedMob = mobOpt.get();
        Team team = gameManager.getTeam(teamName);

        if (team != null) registerActivePlane(team, player, spawnedMob);

        assignOwner(spawnedMob, player);
        player.setGameMode(GameMode.ADVENTURE);

        scheduleMountSignal(spawnedMob, player, "mountPlane");
        scheduler.runLater(() -> {
            if (!spawnedMob.getEntity().getBukkitEntity().getPassengers().contains(player)) {
                spawnedMob.getEntity().getBukkitEntity().addPassenger(player);
            }
        }, 10L);
    }

    /* Helpers */

    private Optional<ActiveMob> spawnMob(String mobName, Location location) {
        return MythicBukkit.inst().getMobManager()
                .getMythicMob(mobName)
                .map(mob -> mob.spawn(BukkitAdapter.adapt(location), 1));
    }

    private void assignOwner(ActiveMob mob, Player player) {
        mob.getEntity().getBukkitEntity()
            .getPersistentDataContainer()
            .set(PdcKeys.OWNER_UUID, PersistentDataType.STRING,
                player.getUniqueId().toString());
    }

    private void preparePlayer(Player player, Location location, Component message) {
        player.getInventory().clear();
        player.teleport(location);
        player.setGameMode(GameMode.ADVENTURE);
        player.sendMessage(message);
        plugin.enableLocatorBarForPlayer(player);
    }

    private void scheduleMountSignal(ActiveMob mob, Player player, String signal) {
        scheduler.runLater(() -> {
            if (mob.isDead() || !player.isOnline()) return;
            mob.signalMob(BukkitAdapter.adapt(player), signal);
        }, 10L);
    }

    private void registerActivePlane(Team team, Player player, ActiveMob planeMob) {
        List<ActiveMob> activePlanes = teamPlaneMaps
                .computeIfAbsent(team, k -> new ArrayList<>());
        int planeIndex = gameManager.getPlaneMembers(team).indexOf(player);

        if (planeIndex < 0) {
            activePlanes.add(planeMob);
            return;
        }

        while (activePlanes.size() <= planeIndex) {
            activePlanes.add(null);
        }

        activePlanes.set(planeIndex, planeMob);
    }

    private void registerHealthManager(Team team, List<ActiveMob> activeMobs) {
        PaperBlimpHealthManager healthManager = new PaperBlimpHealthManager(activeMobs);
        Scheduler.Task task = scheduler.runRepeating(healthManager::update, 0L, 1L);
        blimpHealthTasks.put(team.getName(), task);
        healthManagers.put(team.getName(), healthManager);
    }

}
