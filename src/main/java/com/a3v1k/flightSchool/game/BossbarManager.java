package com.a3v1k.flightSchool.game;

import com.a3v1k.flightSchool.FlightSchool;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;

public class BossbarManager extends BukkitRunnable {

    private final FlightSchool plugin;

    private String title;
    private int currentTicks;
    private int totalTicks;
    private boolean isPaused;
    private BossBar bar;

    public BossbarManager(FlightSchool plugin, int totalTicks, String title, List<Player> playerList) {
        this.plugin = plugin;
        this.currentTicks = totalTicks;
        this.totalTicks = totalTicks;
        this.title = title;
        this.isPaused = false;

        this.bar = Bukkit.createBossBar(title, BarColor.BLUE, BarStyle.SOLID);
        this.bar.setVisible(true);
        for(Player player : playerList) {
            this.bar.addPlayer(player);
        }
    }

    public void setPaused(boolean pause) {
        this.isPaused = pause;
    }

    @Override
    public void run() {
        if(this.currentTicks == 0) {
            this.cancel();

            return;
        }

        if(this.isPaused) {
            return;
        }

        this.update();
    }

    public void update() {
        this.currentTicks -= 1;

        if(this.currentTicks == 0) {
            return;
        }

        this.bar.setTitle(title + " - " + secondsToString(this.currentTicks / 20));
        this.bar.setProgress((double) this.currentTicks / (double) this.totalTicks);
    }

    private String secondsToString(int pTime) {
        return String.format("%02d:%02d", pTime / 60, pTime % 60);
    }

    public String getTime() {
        return secondsToString(this.currentTicks / 20);
    }

}

