package com.a3v1k.flightSchool.listeners;

import com.a3v1k.flightSchool.FlightSchool;
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
        plugin.getGameManager().addPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getGameManager().removePlayer(event.getPlayer());
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (event.getNewGameMode() == GameMode.SPECTATOR) {
            Player player = event.getPlayer();
            GamePlayer gamePlayer = plugin.getGameManager().getGamePlayer(player);
            if (gamePlayer != null && gamePlayer.getTeam() != null) {
                Team team = gamePlayer.getTeam();
                for (Player teamMember : plugin.getServer().getOnlinePlayers()) {
                    GamePlayer teamMemberGamePlayer = plugin.getGameManager().getGamePlayer(teamMember);
                    if (teamMemberGamePlayer != null && teamMemberGamePlayer.getTeam() == team && teamMember != player) {
                        player.setSpectatorTarget(teamMember);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onSpectatorTeleport(PlayerTeleportEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.SPECTATOR && event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) {
            Player spectator = event.getPlayer();
            if (event.getTo().getWorld().getEntities().isEmpty()) return;
            if (!(event.getTo().getWorld().getEntities().get(0) instanceof Player)) return;

            Player target = (Player) event.getTo().getWorld().getEntities().get(0);


            GamePlayer spectatorGamePlayer = plugin.getGameManager().getGamePlayer(spectator);
            GamePlayer targetGamePlayer = plugin.getGameManager().getGamePlayer(target);

            if (spectatorGamePlayer != null && spectatorGamePlayer.getTeam() != null &&
                targetGamePlayer != null && targetGamePlayer.getTeam() != null &&
                spectatorGamePlayer.getTeam() != targetGamePlayer.getTeam()) {
                event.setCancelled(true);
                spectator.sendMessage("You can only spectate your own team!");
            }
        }
    }
}
