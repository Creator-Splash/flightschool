package com.a3v1k.flightSchool.platform.paper.util;

import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import com.a3v1k.flightSchool.domain.match.GameState;
import com.a3v1k.flightSchool.domain.player.GamePlayer;
import com.a3v1k.flightSchool.domain.team.Team;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Vehicle {

    public static FlightSchool plugin = FlightSchool.getInstance();

    public static void explodePlane(Entity entity, Entity damager) {
        MythicBukkit inst = MythicBukkit.inst();
            if (!inst.getMobManager().isMythicMob(entity)) return;

            Optional<ActiveMob> victim = inst.getMobManager().getActiveMob(entity.getUniqueId());
            if (victim.isEmpty()) return;

            ActiveMob victimMob = victim.get();

            if (isPlane(victimMob)) {
                String teamName = extractTeamName(victimMob);
                if (teamName == null) return;
                Team team = plugin.getGameManager().getTeam(teamName);
                NamespacedKey key = new NamespacedKey(plugin, "owner_uuid");

                String uuidString = victimMob.getEntity().getBukkitEntity().getPersistentDataContainer()
                        .get(key, PersistentDataType.STRING);
                UUID uuid = null;
                if (uuidString != null) {
                    uuid = UUID.fromString(uuidString);
                }
                if (uuid == null) return;

                Player player = Bukkit.getPlayer(uuid);
                if (player == null) return;

                if (team != null) {
                    Firework firework = entity.getWorld().spawn(entity.getLocation(), Firework.class);
                    FireworkMeta fireworkMeta = firework.getFireworkMeta();
                    fireworkMeta.addEffect(FireworkEffect.builder()
                            .withColor(team.getColor())
                            .with(FireworkEffect.Type.BALL)
                            .build());
                    firework.setFireworkMeta(fireworkMeta);
                }

                List<Location> planeLocations = plugin.getConfigManager().getPlaneLocations().get(teamName);
                if (planeLocations == null || planeLocations.isEmpty()) return;

                int respawnIndex = 0;
                if (team != null) {
                    int teamIndex = team.getPlaneMembers().indexOf(player);
                    if (teamIndex >= 0) {
                        respawnIndex = Math.min(teamIndex, planeLocations.size() - 1);
                    }
                }

                Location respawnLocation = planeLocations.get(respawnIndex).clone();
                plugin.getKillcamManager().stopRecording(player);
                player.setGameMode(GameMode.SPECTATOR);

                new BukkitRunnable() {
                    int secondsRemaining = 5;

                    @Override
                    public void run() {
                        if (!player.isOnline() || plugin.getGameManager().getGameState() != GameState.IN_GAME) {
                            cancel();
                            return;
                        }

                        if (secondsRemaining <= 0) {
                            cancel();
                            plugin.getGameManager().spawnDelayedPlane(teamName, respawnLocation, player, 0);
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                if (player.isOnline() && player.getGameMode() == GameMode.ADVENTURE) {
                                    plugin.getKillcamManager().startRecording(player);
                                }
                            }, 20L);
                            return;
                        }

                        player.showTitle(Title.title(
                                Component.text("Respawning in " + secondsRemaining, NamedTextColor.GOLD),
                                Component.text(""),
                                0,
                                25,
                                0
                        ));
                        secondsRemaining--;
                    }
                }.runTaskTimer(plugin, 0L, 20L);
                return;
            }

            if (damager == null) return;
            if (!inst.getMobManager().isMythicMob(damager)) return;

            Optional<ActiveMob> attacker = inst.getMobManager().getActiveMob(damager.getUniqueId());
            if (attacker.isEmpty()) return;

            if (!isTurret(victimMob)) return;

            String teamName = extractTeamName(victimMob);
            if (teamName == null) return;
            Team team = plugin.getGameManager().getTeam(teamName);
            NamespacedKey key = new NamespacedKey(plugin, "owner_uuid");

            String uuidString = victimMob.getEntity().getBukkitEntity().getPersistentDataContainer()
                    .get(key, PersistentDataType.STRING);
            if (uuidString == null) return;

            Player player = Bukkit.getPlayer(UUID.fromString(uuidString));
            if (player == null) return;

            player.setGameMode(GameMode.SPECTATOR);
            player.showTitle(Title.title(
                    Component.text("Cannon Destroyed!").color(TextColor.color(255, 0, 0)),
                    Component.text("You are now a spectator."),
                    10,
                    70,
                    20
            ));

            team.increaseDestroyedBlimps();
            if (team.getDestroyedBlimps() != 1) return;

            team.setBlimpDestroyed(true);

            Component message = Component.text("â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•", NamedTextColor.DARK_GREEN)
                    .append(Component.newline())
                    .append(Component.text("Your blimp is destroyed!", NamedTextColor.RED))
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("Your planes now have one final stand!", NamedTextColor.GOLD))
                    .append(Component.newline())
                    .append(Component.text("â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•", NamedTextColor.DARK_GREEN));

            plugin.getGameManager().explodeBlimp(plugin.getGameManager().getBlimp(teamName), team);

            for (UUID uuid1 : team.getMembers()) {
                Player teamMember = Bukkit.getPlayer(uuid1);
                if (teamMember != null) {
                    teamMember.sendMessage(message);
                }
            }

            for (Player planePlayer : team.getPlaneMembers()) {
                GamePlayer gamePlayer = plugin.getGameManager().getGamePlayer(planePlayer);
                if (gamePlayer != null) {
                    gamePlayer.setLastStand(true);
                }
            }
    }

    private static boolean isPlane(ActiveMob activeMob) {
        String faction = activeMob.getFaction();
        return faction != null && faction.contains("plane");
    }

    private static boolean isTurret(ActiveMob activeMob) {
        String faction = activeMob.getFaction();
        return faction != null && faction.contains("turret");
    }

    private static String extractTeamName(ActiveMob activeMob) {
        String beam = activeMob.getVariables().getString("beam");
        if (beam != null && beam.startsWith("beam_") && beam.length() > "beam_".length()) {
            return beam.substring("beam_".length());
        }

        String faction = activeMob.getFaction();
        if (faction != null) {
            if (faction.startsWith("plane_")) {
                return faction.substring("plane_".length());
            }
            if (faction.startsWith("turret_")) {
                return faction.substring("turret_".length());
            }
        }

        return null;
    }

}
