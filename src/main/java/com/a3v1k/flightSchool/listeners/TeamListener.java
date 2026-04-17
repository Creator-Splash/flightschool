package com.a3v1k.flightSchool.listeners;

import com.a3v1k.flightSchool.FlightSchool;
import com.a3v1k.flightSchool.game.GameState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class TeamListener implements Listener {

    private final FlightSchool plugin;

    public TeamListener(FlightSchool plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMeetingListener(PlayerInteractEvent event) {
        if (!(this.plugin.getGameManager().getGameState() == GameState.ROLE_SELECTION)) return;
        if(event.getItem() == null || event.getItem().getType() == Material.AIR) return;
        if(event.getItem().getType() != Material.RECOVERY_COMPASS) return;

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dm open selection_menu " + event.getPlayer().getName());
    }
}
