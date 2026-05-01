package com.a3v1k.flightSchool.application.game;

import com.a3v1k.flightSchool.domain.blimp.BlimpData;
import com.a3v1k.flightSchool.domain.player.GamePlayer;
import com.a3v1k.flightSchool.domain.team.Team;
import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import com.destroystokyo.paper.Title;
import io.lumine.mythic.core.mobs.ActiveMob;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class BlimpHealthManager extends BukkitRunnable {

    private final List<ActiveMob> activeMobs;
    private static final double FALLBACK_TURRET_HEALTH = 150;
    private final double maxHealth;
    @Getter
    private final int initialTurretCount;
    @Getter
    public double health;
    private final Team team;
    @Getter @Setter
    private boolean broken = false;

    private TextDisplay display;

    public BlimpHealthManager(List<ActiveMob> activeMobs, Team team) {
        this.activeMobs = activeMobs;
        this.team = team;
        this.initialTurretCount = Math.max(1, activeMobs.size());
        this.maxHealth = determineMaxHealth(activeMobs);
        this.health = maxHealth;
    }

    @Override
    public void run() {
        update();
    }

    public void update() {
        if (broken) return;

        if (health <= 0) {
            broken = true;
            for (UUID uuid : team.getMembers()) {
                GamePlayer player = FlightSchool.getInstance().getGameManager().getGamePlayer(Bukkit.getPlayer(uuid));
                player.setLastStand(true);
                player.getBukkitPlayer().sendTitle(Title.builder().stay(20).fadeIn(5).fadeOut(5).title(ChatColor.RED + "Blimp Destroyed").subtitle(ChatColor.DARK_RED + "You are on your last stand!").build());
            }
        }
        updateDisplay();
    }

    public void updateDisplay() {
        if (team.getMembers().isEmpty()) return;
        if (display == null) {
            Location displayLocation = getDisplayLocation();

            displayLocation.getWorld().spawn(displayLocation, TextDisplay.class, textDisplay -> {
                this.display = textDisplay;

                Transformation transformation = display.getTransformation();
                transformation.getScale().set(40);

                display.setTransformation(transformation);
                display.setBillboard(Display.Billboard.CENTER);
                display.setShadowed(false);
                display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                display.setViewRange(1000);
            });

        }

        String healthDisplay = getHealthDisplayString();

        display.setText(healthDisplay);

    }

    private Location getDisplayLocation() {
        BlimpData blimp = FlightSchool.getInstance().getGameManager().getBlimp(team.getName());
        if (blimp != null) {
            return blimp.getCenter().clone().add(0, 50, 0);
        }

        return team.getBlimpSpawnLocation().clone().add(0, 50, 0);
    }

    private @NotNull String getHealthDisplayString() {
        if (broken) return ChatColor.RED + "Destroyed";

        String healthDisplay = ChatColor.GREEN + "" + ChatColor.BOLD;
        int healthPercentage = getHealthPercentage();

        int bars = 0;
        while (bars <= healthPercentage / 2) {
            healthDisplay += "|";
            bars++;
        }

        healthDisplay += ChatColor.RED + "" + ChatColor.BOLD;

        while (bars < 50) {
            healthDisplay += "|";
            bars++;
        }
        return healthDisplay;
    }

    public void damage(double damage) {
        if (isBroken() || team.getMembers().isEmpty()) return;
        health -= damage;
        update();
    }

    public boolean hasAllTurretsDestroyed() {
        return team.getDestroyedBlimps() >= initialTurretCount;
    }

    public int getHealthPercentage() {
        return (int) Math.ceil((health / maxHealth) * 100);
    }

    private double determineMaxHealth(List<ActiveMob> activeMobs) {
        double totalHealth = 0;

        for (ActiveMob activeMob : activeMobs) {
            if (activeMob == null || activeMob.getEntity() == null) {
                totalHealth += FALLBACK_TURRET_HEALTH;
                continue;
            }

            if (activeMob.getEntity().getBukkitEntity() instanceof LivingEntity livingEntity) {
                totalHealth += livingEntity.getMaxHealth();
            } else {
                totalHealth += FALLBACK_TURRET_HEALTH;
            }
        }

        return totalHealth <= 0 ? FALLBACK_TURRET_HEALTH : totalHealth;
    }

    public void disable() {
        this.cancel();
    }

    public void disableAndDespawn() {
        disable();
        if (display != null) {
            display.remove();
        }

        for (ActiveMob activeMob : this.activeMobs) {
            if (activeMob == null || activeMob.getEntity() == null) {
                continue;
            }

            activeMob.despawn();
        }
    }
}
