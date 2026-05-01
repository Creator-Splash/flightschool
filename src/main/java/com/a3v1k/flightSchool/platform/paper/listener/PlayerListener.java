package com.a3v1k.flightSchool.platform.paper.listener;

import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import com.a3v1k.flightSchool.domain.match.GameState;
import com.a3v1k.flightSchool.domain.player.GamePlayer;
import com.a3v1k.flightSchool.domain.player.Role;
import com.a3v1k.flightSchool.domain.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.UUID;

public class PlayerListener implements Listener {
    private final FlightSchool plugin;

    public PlayerListener(FlightSchool plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getGameManager().addPlayer(player);

        if(plugin.getGameManager().getGameState() != GameState.LOBBY) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.enableLocatorBarForPlayer(player);
                plugin.getTeamVisualManager().refreshAll();
            });
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.resetPlayerToLobby(player, false);
            plugin.getTeamVisualManager().refreshAll();
        });
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        plugin.enableLocatorBarOnAllWorlds();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getGameManager().removePlayer(event.getPlayer());
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getTeamVisualManager().refreshAll());
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> plugin.enableLocatorBarForPlayer(event.getPlayer()));

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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlanePilotSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        if (!shouldKeepPlanePilotMounted(event.getPlayer())) return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlanePilotDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!shouldKeepPlanePilotMounted(player)) return;

        event.setCancelled(true);
    }

    private boolean shouldKeepPlanePilotMounted(Player player) {
        if (plugin.getGameManager().getGameState() != GameState.IN_GAME) return false;
        if (player.getGameMode() != GameMode.ADVENTURE) return false;
        if (!player.isInsideVehicle()) return false;

        GamePlayer gamePlayer = plugin.getGameManager().getGamePlayer(player);
        if (gamePlayer == null || gamePlayer.getRole() != Role.PLANE_PILOT) return false;

        return !gamePlayer.isEliminated() && !gamePlayer.isLastStand();
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

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        player.setExhaustion(0);
    }

    @EventHandler
    public void onFoodExhaustion(FoodLevelChangeEvent e) {
        e.setCancelled(true);
    }

}
