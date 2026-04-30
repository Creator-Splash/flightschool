package com.a3v1k.flightSchool.application.game;

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
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class BlimpHealthManager extends BukkitRunnable {

    private final List<ActiveMob> activeMobs;
    private static final double maxHealth = 500;
    @Getter
    public double health = maxHealth;
    private final Team team;
    @Getter @Setter
    private boolean broken = false;

    private TextDisplay display;

    public BlimpHealthManager(List<ActiveMob> activeMobs, Team team) {
        this.activeMobs = activeMobs;
        this.team = team;
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
            Location displayLocation = team.getBlimpSpawnLocation().clone().add(20, 45, 100);

            displayLocation.getWorld().spawn(displayLocation, TextDisplay.class, textDisplay -> {
                this.display = textDisplay;

                Transformation transformation = display.getTransformation();
                transformation.getScale().set(15);

                display.setTransformation(transformation);
                display.setBillboard(Display.Billboard.CENTER);
                display.setShadowed(false);
                display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            });

        }

        String healthDisplay = getHealthDisplayString();

        display.setText(healthDisplay);

    }

    private @NotNull String getHealthDisplayString() {
        if (broken) return ChatColor.RED + "Destroyed";

        String healthDisplay = ChatColor.GREEN + "" + ChatColor.BOLD;
        int healthPercentage = getHealthPercentage();

        int bars = 0;
        while (bars <= healthPercentage / 10) {
            healthDisplay += "|";
            bars++;
        }

        healthDisplay += ChatColor.GRAY + "" + ChatColor.BOLD;

        while (bars < 10) {
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

    public int getHealthPercentage() {
        return (int) Math.ceil((health / maxHealth) * 100);
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
