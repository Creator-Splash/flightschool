package com.a3v1k.flightSchool.platform.paper.listener;

import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import com.a3v1k.flightSchool.domain.match.GameState;
import com.a3v1k.flightSchool.application.game.WarningManager;
import com.a3v1k.flightSchool.domain.player.GamePlayer;
import com.a3v1k.flightSchool.domain.team.Team;
import com.a3v1k.flightSchool.platform.paper.util.Vehicle;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicProjectileHitEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
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
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.event.FocusEvent;
import java.util.HashMap;
import java.util.List;
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

        MythicBukkit inst = MythicBukkit.inst();
        boolean isVictimMythic = inst.getMobManager().isMythicMob(entity);
        boolean isAttackerMythic = inst.getMobManager().isMythicMob(damager);

        if (!isVictimMythic || !isAttackerMythic) return;

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

        if (!isFaction(victim.get(), "turret")) {
            return;
        }

        Team team = this.plugin.getGameManager().getTeam(victimTeamName);
        if (team == null) {
            return;
        }

        this.plugin.getLogger().info("[Detected Turret Damage] Team Turret: " + team.getName() + " || Team attacking: " + attackingTeamName);

        for (Player player : team.getPlaneMembers()) {
            if (warningManagerMap.containsKey(player)) continue;

            warningManagerMap.put(player, new WarningManager(player));
            warningManagerMap.get(player).runTaskTimer(this.plugin, 0, 1);
        }
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
