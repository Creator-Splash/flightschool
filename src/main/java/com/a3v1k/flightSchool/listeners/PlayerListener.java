package com.a3v1k.flightSchool.listeners;

import com.a3v1k.flightSchool.FlightSchool;
import com.a3v1k.flightSchool.game.GameState;
import com.a3v1k.flightSchool.player.GamePlayer;
import com.a3v1k.flightSchool.team.Team;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerListener implements Listener {

    private final FlightSchool plugin;

    public PlayerListener(FlightSchool plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if(plugin.getGameManager().getGameState() == GameState.LOBBY){
            event.getPlayer().getInventory().clear();
        }

        plugin.getGameManager().addPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getGameManager().removePlayer(event.getPlayer());
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if(event.getNewGameMode() != GameMode.SPECTATOR) return;

        Player player = event.getPlayer();
        GamePlayer gamePlayer = plugin.getGameManager().getGamePlayer(player);
        if (gamePlayer == null || gamePlayer.getTeam() == null) return;

        Team team = gamePlayer.getTeam();
        for (Player teamMember : plugin.getServer().getOnlinePlayers()) {
            GamePlayer teamMemberGamePlayer = plugin.getGameManager().getGamePlayer(teamMember);
            if (teamMemberGamePlayer == null || teamMemberGamePlayer.getTeam() != team || teamMember == player) continue;

            player.setSpectatorTarget(teamMember);
            return;
        }
    }

    @EventHandler
    public void onSpectatorTeleport(PlayerTeleportEvent event) {
        if(event.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE) return;
        if(event.getPlayer().getGameMode() != GameMode.SPECTATOR) return;

        Player spectator = event.getPlayer();
        if (event.getTo().getWorld().getEntities().isEmpty()) return;
        if (!(event.getTo().getWorld().getEntities().getFirst() instanceof Player target)) return;

        GamePlayer spectatorGamePlayer = plugin.getGameManager().getGamePlayer(spectator);
        GamePlayer targetGamePlayer = plugin.getGameManager().getGamePlayer(target);

        if(spectatorGamePlayer == null || targetGamePlayer == null) return;
        if(spectatorGamePlayer.getTeam() == null || targetGamePlayer.getTeam() == null) return;
        if(spectatorGamePlayer.getTeam() == targetGamePlayer.getTeam()) return;

        event.setCancelled(true);
        spectator.sendMessage("You can only spectate your own team!");
    }
}
