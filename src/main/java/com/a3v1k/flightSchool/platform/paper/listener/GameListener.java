package com.a3v1k.flightSchool.platform.paper.listener;

import com.a3v1k.flightSchool.application.scheduler.Scheduler;
import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import com.a3v1k.flightSchool.domain.match.GameState;
import com.a3v1k.flightSchool.platform.paper.game.PaperWarningManager;
import com.a3v1k.flightSchool.domain.team.Team;
import com.a3v1k.flightSchool.platform.paper.util.Vehicle;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class GameListener implements Listener {

    private final FlightSchool plugin;

    private final Map<Player, PaperWarningManager> warningManagerMap = new HashMap<>();

    public GameListener(FlightSchool plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamageCannon(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        Entity damager = event.getDamager();

        MythicBukkit inst = MythicBukkit.inst();
        if (!inst.getMobManager().isMythicMob(entity)) return;
        if (!inst.getMobManager().isMythicMob(damager)) return;

        Optional<ActiveMob> victim = inst.getMobManager().getActiveMob(entity.getUniqueId());
        Optional<ActiveMob> attacker = inst.getMobManager().getActiveMob(damager.getUniqueId());
        if (victim.isEmpty() || attacker.isEmpty()) return;
        if (!isFaction(victim.get(), "turret") && !isFaction(victim.get(), "plane")) return;
        if (!isFaction(attacker.get(), "plane")) return;

        String victimTeamName = extractTeamName(victim.get());
        String attackingTeamName = extractTeamName(attacker.get());

        if (victimTeamName != null && victimTeamName.equalsIgnoreCase(attackingTeamName)) {
            event.setCancelled(true);
            return;
        }

        if (!isFaction(victim.get(), "turret")) return;

        Team team = plugin.getGameManager().getTeam(victimTeamName);
        if (team == null) return;

        plugin.getLogger().info("[Detected Turret Damage] Team Turret: %s || Team attacking: %s"
            .formatted(team.getName(), attackingTeamName));

        for (Player player : plugin.getGameManager().getPlaneMembers(team)) {
            if (warningManagerMap.containsKey(player)) continue;

            PaperWarningManager warningManager = new PaperWarningManager(player);
            warningManagerMap.put(player, warningManager);

            plugin.getScheduler().runRepeating(t -> {
                warningManager.update();
                if (warningManager.isFinished()) {
                    t.cancel();
                    warningManagerMap.remove(player);
                }
            }, 0L, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onModelDamage(EntityDamageEvent event) {
        if (plugin.getGameManager().getGameState() != GameState.IN_GAME) return;

        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity livingEntity)) return;

        MythicBukkit mythic = MythicBukkit.inst();
        if (!mythic.getMobManager().isMythicMob(entity)) return;

        Optional<ActiveMob> activeMob = mythic.getMobManager().getActiveMob(entity.getUniqueId());
        if (activeMob.isEmpty()) return;

        ActiveMob mob = activeMob.get();
        if (!isFaction(mob, "plane") && !isFaction(mob, "turret")) return;

        NamespacedKey key = new NamespacedKey(plugin, "owner_uuid");
        String uuidString = entity.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (uuidString == null) return;

        Player player = Bukkit.getPlayer(UUID.fromString(uuidString));
        if (player == null || !player.isOnline()) return;

        AttributeInstance modelMaxHealthAttr = livingEntity.getAttribute(Attribute.MAX_HEALTH);
        AttributeInstance playerMaxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);

        if (modelMaxHealthAttr == null || playerMaxHealthAttr == null) return;

        double modelMaxHealth = modelMaxHealthAttr.getValue();
        double modelCurrentHealth = Math.max(0, livingEntity.getHealth() - event.getFinalDamage());
        double playerMaxHealth = playerMaxHealthAttr.getValue();
        double newPlayerHealth = Math.max(0.1, (modelCurrentHealth / modelMaxHealth) * playerMaxHealth);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.setHealth(newPlayerHealth);
            }
        });
    }

    @EventHandler
    public void onKillPlane(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        Entity damager = event.getDamageSource().getDirectEntity();
        Vehicle.explodePlane(entity, damager);
    }

    @EventHandler
    public void onPlayerAttackFromEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof LivingEntity)) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player)) return;
        if (plugin.getGameManager().getGameState() != GameState.IN_GAME) return;

        org.bukkit.entity.Vehicle bukkitVehicle = event.getVehicle();

        MythicBukkit mythic = MythicBukkit.inst();
        if (!mythic.getMobManager().isMythicMob(bukkitVehicle)) return;

        Optional<ActiveMob> activeMob = mythic.getMobManager().getActiveMob(bukkitVehicle.getUniqueId());
        if (activeMob.isEmpty()) return;

        String faction = activeMob.get().getFaction();
        if (faction != null && faction.contains("plane")) {
            event.setCancelled(true);
        }
    }

    /* Internals */

    private boolean isFaction(ActiveMob mob, String factionKey) {
        String faction = mob.getFaction();
        return faction != null && faction.contains(factionKey);
    }

    private String extractTeamName(ActiveMob mob) {
        String beam = mob.getVariables().getString("beam");
        if (beam != null && beam.startsWith("beam_") && beam.length() > "beam_".length()) {
            return beam.substring("beam_".length());
        }

        String faction = mob.getFaction();
        if (faction == null) {
            return null;
        }

        if (faction.startsWith("plane_")) {
            return faction.substring("plane_".length());
        }

        if (faction.startsWith("turret_")) {
            return faction.substring("turret_".length());
        }

        return null;
    }
}
