package com.a3v1k.flightSchool.game;

import com.a3v1k.flightSchool.FlightSchool;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;

public class BlimpHealthManager extends BukkitRunnable {

    private final List<ActiveMob> activeMobs;

    public BlimpHealthManager(List<ActiveMob> activeMobs) {
        this.activeMobs = activeMobs;
    }

    @Override
    public void run() {
        update();
    }

    public void update() {

    }

    public double getHealth() {
        double netHealth = 0.0;
        double maxHealth = 0.0;

        for(ActiveMob activeMob : this.activeMobs) {
            netHealth += activeMob.getEntity().getHealth();
            maxHealth += activeMob.getEntity().getMaxHealth();
        }

        double health = netHealth / maxHealth * 100;
        return Double.isNaN(health) ? 0 : health;
    }

    public void disable() {
        this.cancel();
    }

    public void disableAndDespawn() {
        disable();

        for (ActiveMob activeMob : this.activeMobs) {
            if (activeMob == null || activeMob.getEntity() == null) {
                continue;
            }

            activeMob.despawn();
        }
    }
}
