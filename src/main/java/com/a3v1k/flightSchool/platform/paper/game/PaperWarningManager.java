package com.a3v1k.flightSchool.platform.paper.game;

import com.a3v1k.flightSchool.application.game.WarningManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class PaperWarningManager implements WarningManager {

    private static final int WARNING_DURATION_TICKS = 100;
    private static final Component TITLE_RED =
        Component.text("ʙʟɪᴍᴘ ᴜɴᴅᴇʀ ᴀᴛᴛᴀᴄᴋ", NamedTextColor.RED);
    private static final Component TITLE_YELLOW =
        Component.text("ʙʟɪᴍᴘ ᴜɴᴅᴇʀ ᴀᴛᴛᴀᴄᴋ", NamedTextColor.YELLOW);

    private final BossBar bar;
    private final Player player;
    private int warningTicks = WARNING_DURATION_TICKS;

    public PaperWarningManager(Player player) {
        this.player = player;
        this.bar = BossBar.bossBar(TITLE_RED, 1f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        player.showBossBar(bar);
    }

    @Override
    public void update() {
        if (warningTicks <= 0) {
            stop();
            return;
        }

        warningTicks--;

        boolean alternate = (warningTicks / 20) % 2 == 0;
        bar.color(alternate ? BossBar.Color.YELLOW : BossBar.Color.RED);
        bar.progress(alternate ? 0f : 1f);
        bar.name(alternate ? TITLE_YELLOW : TITLE_RED);
    }

    @Override
    public void stop() {
        player.hideBossBar(bar);
    }

    public boolean isFinished() {
        return warningTicks <= 0;
    }
}
