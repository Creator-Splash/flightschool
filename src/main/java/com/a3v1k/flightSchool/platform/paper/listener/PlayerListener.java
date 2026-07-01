package com.a3v1k.flightSchool.platform.paper.listener;

import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import com.a3v1k.flightSchool.domain.match.GameState;
import com.a3v1k.flightSchool.domain.player.GamePlayer;
import com.a3v1k.flightSchool.domain.player.Role;
import com.a3v1k.flightSchool.domain.team.Team;
import com.ticxo.modelengine.api.events.ModelDismountEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class PlayerListener implements Listener {
    private final FlightSchool plugin;

    public PlayerListener(FlightSchool plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getGameManager().addPlayer(player.getUniqueId());

        if (plugin.getGameManager().getGameState() != GameState.LOBBY) {
            plugin.getScheduler().run(() -> {
                plugin.enableLocatorBarForPlayer(player);
                plugin.getTeamVisualManager().refreshAll();
            });
            return;
        }

        plugin.getScheduler().run(() -> {
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
        plugin.getGameManager().removePlayer(event.getPlayer().getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getTeamVisualManager().refreshAll());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (plugin.getGameManager().getGameState() != GameState.IN_GAME) return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.VOID ||
            cause == EntityDamageEvent.DamageCause.WORLD_BORDER) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> plugin.enableLocatorBarForPlayer(event.getPlayer()));

        if(event.getNewGameMode() != GameMode.SPECTATOR) return;

        Player player = event.getPlayer();
        GamePlayer gamePlayer = plugin.getGameManager().getGamePlayer(player.getUniqueId());
        if (gamePlayer == null || gamePlayer.getTeam() == null) return;

        Team team = gamePlayer.getTeam();
        for (Player teamMember : plugin.getServer().getOnlinePlayers()) {
            GamePlayer teamMemberGamePlayer = plugin.getGameManager().getGamePlayer(teamMember.getUniqueId());
            if (teamMemberGamePlayer == null || teamMemberGamePlayer.getTeam() != team || teamMember == player) continue;

            player.setSpectatorTarget(teamMember);
            return;
        }
    }

    // The pilot rides a ModelEngine seat bone, so sneaking dismounts via ModelEngine's
    // own ModelDismountEvent (cancellable, aborts the dismount in MountManagerImpl), not
    // the vanilla VehicleExit/EntityDismount events. ModelEngine reads the raw sneak input
    // packet each tick, before PlayerToggleSneakEvent, so cancelling this event is the only
    // hook that actually keeps the pilot mounted.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlaneModelDismount(ModelDismountEvent event) {
        if (!(event.getPassenger() instanceof Player player)) return;
        if (!shouldKeepPlanePilotMounted(player)) return;

        event.setCancelled(true);
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

        GamePlayer gamePlayer = plugin.getGameManager().getGamePlayer(player.getUniqueId());
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

        GamePlayer spectatorGamePlayer = plugin.getGameManager().getGamePlayer(spectator.getUniqueId());
        GamePlayer targetGamePlayer = plugin.getGameManager().getGamePlayer(target.getUniqueId());

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
