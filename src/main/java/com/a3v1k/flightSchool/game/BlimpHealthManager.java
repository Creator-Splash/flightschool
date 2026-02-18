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

    private final FlightSchool plugin;

    private String title;
    private boolean isPaused;
    private List<ActiveMob> activeMobs;
    private BossBar bar;

    public BlimpHealthManager(FlightSchool plugin, List<ActiveMob> activeMobs, String title, List<UUID> playerList) {
        this.plugin = plugin;
        this.activeMobs = activeMobs;
        this.title = title;
        this.isPaused = false;

        this.bar = Bukkit.createBossBar(title, BarColor.BLUE, BarStyle.SOLID);
        this.bar.setVisible(true);
        for(UUID uuid : playerList) {
            this.bar.addPlayer(Bukkit.getPlayer(uuid));
        }
    }

    @Override
    public void run() {
        update();
    }

    public void update() {
        double netHealth = 0.0;
        double maxHealth = 0.0;

        for(ActiveMob activeMob : this.activeMobs) {
            netHealth += activeMob.getEntity().getHealth();
            maxHealth += activeMob.getEntity().getMaxHealth();
        }
        double percentage = netHealth / maxHealth * 100;
        this.plugin.getLogger().info("Health: " + percentage);

        this.bar.setProgress(percentage / 100);
        this.bar.setTitle(this.title + " - " + String.format("%.2f", percentage) + "%");

        if(percentage == 0.0) {
            this.bar.removeAll();
        }
    }

    public double getHealth() {
        double netHealth = 0.0;
        double maxHealth = 0.0;

        for(ActiveMob activeMob : this.activeMobs) {
            netHealth += activeMob.getEntity().getHealth();
            maxHealth += activeMob.getEntity().getMaxHealth();
        }
        double percentage = netHealth / maxHealth * 100;

        return percentage;
    }

    public void disable() {
        this.cancel();
        this.bar.removeAll();
    }
}
