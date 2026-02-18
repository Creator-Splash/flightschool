package com.a3v1k.flightSchool.listeners;

import com.a3v1k.flightSchool.FlightSchool;
import com.a3v1k.flightSchool.game.WarningManager;
import com.a3v1k.flightSchool.player.GamePlayer;
import com.a3v1k.flightSchool.team.Team;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class GameListener implements Listener {

    private final FlightSchool plugin;
    private final Map<Player, WarningManager> warningManagerMap = new HashMap<>();

    public GameListener(FlightSchool plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamageCannon(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        Entity damager = event.getDamager();

        boolean isMythicMob_e = MythicBukkit.inst().getMobManager().isMythicMob(entity);
        boolean isMythicMob_d = MythicBukkit.inst().getMobManager().isMythicMob(damager);

        if(isMythicMob_e && isMythicMob_d) {
            Optional<ActiveMob> optionalActiveMob = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId());
            Optional<ActiveMob> optionalActiveMob_d = MythicBukkit.inst().getMobManager().getActiveMob(damager.getUniqueId());
            // Get entity name and check if its a turret
            if(optionalActiveMob.isPresent() && optionalActiveMob.get().getFaction().contains("turret")
                && optionalActiveMob_d.isPresent() && optionalActiveMob_d.get().getFaction().contains("plane")) {
                ActiveMob activeMob = optionalActiveMob.get();
                ActiveMob activeMob_d = optionalActiveMob_d.get();

                // Get players who are flying the planes
                String teamName = activeMob.getVariables().getString("beam").split("beam_")[1];
                Team team = this.plugin.getGameManager().getTeam(teamName);

                String attackingTeamName = activeMob_d.getVariables().getString("beam").split("beam_")[1];
                Team attackingTeam = this.plugin.getGameManager().getTeam(attackingTeamName);

                // Get person who owns it
                this.plugin.getLogger().info("[Detected Turret Damage] Team Turret: " + team.getName() + " || Team attacking: " + attackingTeamName);

                if(team != null) {
                    for(Player player : team.getPlaneMembers()) {
                        if(!warningManagerMap.containsKey(player)) {
                            warningManagerMap.put(player, new WarningManager(this.plugin, player));
                            warningManagerMap.get(player).runTaskTimer(this.plugin, 0, 1);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onKillPlane(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        Entity damager = event.getDamageSource().getDirectEntity();


        boolean isMythicMob_e = MythicBukkit.inst().getMobManager().isMythicMob(entity);
        boolean isMythicMob_d = MythicBukkit.inst().getMobManager().isMythicMob(damager);

        if(isMythicMob_e && isMythicMob_d) {
            Optional<ActiveMob> activeMob_e = MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId());
            Optional<ActiveMob> activeMob_d = MythicBukkit.inst().getMobManager().getActiveMob(damager.getUniqueId());

            if(!activeMob_e.isPresent() || !activeMob_d.isPresent()) return;

            io.lumine.mythic.core.mobs.ActiveMob activeMobE = activeMob_e.get();
            io.lumine.mythic.core.mobs.ActiveMob activeMobD = activeMob_d.get();

            // Get players who are flying the planes
            String teamName = activeMobE.getVariables().getString("beam").split("beam_")[1];
            Team team = this.plugin.getGameManager().getTeam(teamName);

            String attackingTeamName = activeMobD.getVariables().getString("beam").split("beam_")[1];
            Team attackingTeam = this.plugin.getGameManager().getTeam(attackingTeamName);

            // Get entity name and check if its a plane
            if(activeMobE.getFaction().contains("plane")) {
                NamespacedKey key = new NamespacedKey(plugin, "owner_uuid");

                String uuidString = activeMobE.getEntity().getBukkitEntity().getPersistentDataContainer()
                        .get(key, PersistentDataType.STRING);
                UUID uuid = null;
                if (uuidString != null) {
                    uuid = UUID.fromString(uuidString);
                }
                if(uuid == null) return;
                Player player = Bukkit.getPlayer(uuid);
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage(ChatColor.GOLD + "You will respawn in 15 seconds.");

                if (team != null) {
                    // Spawn a firework of the team's color
                    Firework firework = entity.getWorld().spawn(entity.getLocation(), Firework.class);
                    FireworkMeta fireworkMeta = firework.getFireworkMeta();
                    fireworkMeta.addEffect(FireworkEffect.builder()
                        .withColor(team.getColor())
                        .with(FireworkEffect.Type.BALL)
                        .build());
                    firework.setFireworkMeta(fireworkMeta);
                }
                this.plugin.getKillcamManager().playKillcam(player, teamName);
//                this.plugin.getGameManager().spawnDelayedPlane(teamName, this.plugin.getConfigManager().getPlaneLocations().get(teamName).get(0), player, 15);
            }

            if(activeMobE.getFaction().contains("turret")) {
                NamespacedKey key = new NamespacedKey(plugin, "owner_uuid");

                String uuidString = activeMobE.getEntity().getBukkitEntity().getPersistentDataContainer()
                        .get(key, PersistentDataType.STRING);
                UUID uuid = null;
                if (uuidString != null) {
                    uuid = UUID.fromString(uuidString);
                }
                if(uuid == null) return;
                Player deadPlayer = Bukkit.getPlayer(uuid);
                deadPlayer.setGameMode(GameMode.SPECTATOR);
                deadPlayer.sendTitle(ChatColor.RED + "Cannon Destroyed!", "You are now a spectator.", 10, 70, 20);

                team.increaseDestroyedBlimps();
                if(team.getDestroyedBlimps() == 1) { // TODO: Make it 2
                    team.setBlimpDestroyed(true);

                    Component message =
                            Component.text("════════════════════════════════", NamedTextColor.DARK_GREEN)
                                    .append(Component.newline())
                                    .append(
                                            Component.text("Your blimp is destroyed!", NamedTextColor.RED)
                                    )
                                    .append(Component.newline())
                                    .append(Component.newline())
                                    .append(
                                            Component.text("Your planes now have one final stand!", NamedTextColor.GOLD)
                                    )
                                    .append(Component.newline())
                                    .append(Component.text("════════════════════════════════", NamedTextColor.DARK_GREEN));

                    this.plugin.getGameManager().explodeBlimp(this.plugin.getGameManager().getBlimp(teamName), team);
                    for(UUID uuid1 : team.getMembers()) {
                        Player player = Bukkit.getPlayer(uuid1);
                        player.sendMessage(message);
                    }

                    for(Player player : team.getPlaneMembers()) {
                        GamePlayer gamePlayer = this.plugin.getGameManager().getGamePlayer(player);
                        if (gamePlayer != null) {
                            gamePlayer.setLastStand(true);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerAttackFromEntity(EntityDamageByEntityEvent event) {
        if(event.getEntity() instanceof Player player) {
            if(event.getDamager() instanceof LivingEntity livingEntity) {
                event.setCancelled(true);
            }
        }
    }
}
