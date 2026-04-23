package com.a3v1k.flightSchool.application.game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class WarningManager extends BukkitRunnable {

    private int warningTime = 100;
    private final BossBar bar;
    private final String title;

    public WarningManager(Player player) {
        this.title = "ʙʟɪᴍᴘ ᴜɴᴅᴇʀ ᴀᴛᴛᴀᴄᴋ";
        this.bar = Bukkit.createBossBar(title, BarColor.RED, BarStyle.SOLID);
        this.bar.setVisible(true);
        this.bar.addPlayer(player);
    }

    @Override
    public void run() {
        if(warningTime == 0) {
            this.cancel();
            this.bar.setVisible(false);
            this.bar.removeAll();
        }

        warningTime -= 1;
        ChatColor current = ChatColor.RED;
        float progress = 1.0f;
        if((int) (warningTime / 20) % 2 == 0) {
            current = ChatColor.YELLOW;
            progress = 0f;
            this.bar.setColor(BarColor.YELLOW);
        }
        this.bar.setTitle(current + title);
        this.bar.setProgress(progress);
    }
}
