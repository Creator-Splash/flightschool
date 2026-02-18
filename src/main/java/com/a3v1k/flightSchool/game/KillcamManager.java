package com.a3v1k.flightSchool.killcam;

import com.a3v1k.flightSchool.FlightSchool;
import me.jumper251.replay.api.ReplayAPI;
import me.jumper251.replay.api.ReplaySessionFinishEvent;
import me.jumper251.replay.replaysystem.Replay;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KillcamManager implements Listener {

    private final FlightSchool plugin;

    private final Map<UUID, String> activeReplayNames = new HashMap<>();
    private final Map<UUID, String> pendingRespawns = new HashMap<>();

    private static final int WINDOW_SECONDS = 10;

    public KillcamManager(FlightSchool plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /*
     * Start continuous rolling recording for player
     */
    public void startRecording(Player player) {

        UUID uuid = player.getUniqueId();
        String replayName = "live_" + uuid;

        activeReplayNames.put(uuid, replayName);

        startNewRecording(player, replayName);

        // Restart recording every 10 seconds to simulate rolling buffer
        new BukkitRunnable() {
            @Override
            public void run() {

                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (!activeReplayNames.containsKey(uuid)) {
                    cancel();
                    return;
                }

                // Stop old recording (save)
                ReplayAPI.getInstance().stopReplay(replayName, false, true);

                // Start new one
                startNewRecording(player, replayName);

            }
        }.runTaskTimer(plugin, WINDOW_SECONDS * 20L, WINDOW_SECONDS * 20L);
    }

    private void startNewRecording(Player player, String replayName) {
        ReplayAPI.getInstance().recordReplay(replayName, player);
    }

    /*
     * Called on plane death
     */
    public void playKillcam(Player deadPlayer, String teamName) {

        UUID uuid = deadPlayer.getUniqueId();
        String replayName = activeReplayNames.get(uuid);

        if (replayName == null) return;

        // Stop recording and save last window
        ReplayAPI.getInstance().stopReplay(replayName, true, true);

        deadPlayer.setGameMode(GameMode.SPECTATOR);

        pendingRespawns.put(uuid, teamName);

        // Small delay ensures replay file finishes saving
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ReplayAPI.getInstance().playReplay(replayName, deadPlayer);
        }, 10L);
    }

    /*
     * When replay ends → respawn plane and resume recording
     */
    @EventHandler
    public void onReplayFinished(ReplaySessionFinishEvent event) {

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!pendingRespawns.containsKey(uuid))
            return;

        String teamName = pendingRespawns.remove(uuid);

        player.setGameMode(GameMode.SURVIVAL);

        plugin.getGameManager().spawnDelayedPlane(
                teamName,
                plugin.getConfigManager().getPlaneLocations().get(teamName).get(0),
                player,
                0
        );

        // Restart rolling recording
        startRecording(player);
    }

    /*
     * Optional cleanup
     */
    public void stopRecording(Player player) {

        UUID uuid = player.getUniqueId();
        String replayName = activeReplayNames.remove(uuid);

        if (replayName != null) {
            ReplayAPI.getInstance().stopReplay(replayName, false);
        }
    }
}
