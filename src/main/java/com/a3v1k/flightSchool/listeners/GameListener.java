package com.a3v1k.flightSchool.listeners;

import com.a3v1k.flightSchool.FlightSchool;
import com.a3v1k.flightSchool.game.WarningManager;
import com.a3v1k.flightSchool.player.GamePlayer;
import com.a3v1k.flightSchool.team.Team;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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

        try(MythicBukkit inst = MythicBukkit.inst()){
            boolean isVictimMythic = inst.getMobManager().isMythicMob(entity);
            boolean isAttackerMythic = inst.getMobManager().isMythicMob(damager);

            if(!isVictimMythic || !isAttackerMythic) return;

            Optional<ActiveMob> victim = inst.getMobManager().getActiveMob(entity.getUniqueId());
            Optional<ActiveMob> attacker = inst.getMobManager().getActiveMob(damager.getUniqueId());
            if(victim.isEmpty() || attacker.isEmpty()) return;
            if(!victim.get().getFaction().contains("turret") || !attacker.get().getFaction().contains("plane")) return;


            // Get players who are flying the planes
            String teamName = victim.get().getVariables().getString("beam").split("beam_")[1];
            Team team = this.plugin.getGameManager().getTeam(teamName);

            String attackingTeamName = attacker.get().getVariables().getString("beam").split("beam_")[1];
            Team attackingTeam = this.plugin.getGameManager().getTeam(attackingTeamName);

            // Get person who owns it
            this.plugin.getLogger().info("[Detected Turret Damage] Team Turret: " + team.getName() + " || Team attacking: " + attackingTeamName);

            for (Player player : team.getPlaneMembers()) {
                if(warningManagerMap.containsKey(player)) continue;

                warningManagerMap.put(player, new WarningManager(player));
                warningManagerMap.get(player).runTaskTimer(this.plugin, 0, 1);
            }
        }
    }

    @EventHandler
    public void onKillPlane(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        Entity damager = event.getDamageSource().getDirectEntity();

        if(damager == null) return;

        try(MythicBukkit inst = MythicBukkit.inst()){
            boolean isVictimMythic = inst.getMobManager().isMythicMob(entity);
            boolean isAttackerMythic = inst.getMobManager().isMythicMob(damager);

            if(!isVictimMythic || !isAttackerMythic) return;

            Optional<ActiveMob> victim = inst.getMobManager().getActiveMob(entity.getUniqueId());
            Optional<ActiveMob> attacker = inst.getMobManager().getActiveMob(damager.getUniqueId());

            if(victim.isEmpty() || attacker.isEmpty()) return;

            ActiveMob victimMob = victim.get();
            ActiveMob attackerMob = attacker.get();

            // Get players who are flying the planes
            String teamName = victimMob.getVariables().getString("beam").split("beam_")[1];
            Team team = this.plugin.getGameManager().getTeam(teamName);

            String attackingTeamName = attackerMob.getVariables().getString("beam").split("beam_")[1];
            Team attackingTeam = this.plugin.getGameManager().getTeam(attackingTeamName);

            // Get entity name and check if its a plane
            if(victimMob.getFaction().contains("plane")) {
                NamespacedKey key = new NamespacedKey(plugin, "owner_uuid");

                String uuidString = victimMob.getEntity().getBukkitEntity().getPersistentDataContainer()
                        .get(key, PersistentDataType.STRING);
                UUID uuid = null;
                if (uuidString != null) {
                    uuid = UUID.fromString(uuidString);
                }
                if(uuid == null) return;
                Player player = Bukkit.getPlayer(uuid);

                if(player == null) return;
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
                return;
            }

            if(!victimMob.getFaction().contains("turret")) return;

            NamespacedKey key = new NamespacedKey(plugin, "owner_uuid");

            String uuidString = victimMob.getEntity().getBukkitEntity().getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if(uuidString == null) return;

            UUID uuid = UUID.fromString(uuidString);
            Player deadPlayer = Bukkit.getPlayer(uuid);
            if(deadPlayer == null) return;

            deadPlayer.setGameMode(GameMode.SPECTATOR);
            deadPlayer.showTitle(Title.title(Component.text("Cannon Destroyed!").color(TextColor.color(255, 0, 0)), Component.text("You are now a spectator."), 10, 70, 20));

            team.increaseDestroyedBlimps();
            if(team.getDestroyedBlimps() != 1) return;

            team.setBlimpDestroyed(true);

            Component message = Component.text("════════════════════════════════", NamedTextColor.DARK_GREEN)
                    .append(Component.newline())
                    .append(Component.text("Your blimp is destroyed!", NamedTextColor.RED))
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("Your planes now have one final stand!", NamedTextColor.GOLD))
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

    @EventHandler
    public void onPlayerAttackFromEntity(EntityDamageByEntityEvent event) {
        if(!(event.getEntity() instanceof Player)) return;
        if(!(event.getDamager() instanceof LivingEntity)) return;

        event.setCancelled(true);
    }
}
