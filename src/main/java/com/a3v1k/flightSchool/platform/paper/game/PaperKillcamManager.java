package com.a3v1k.flightSchool.platform.paper.game;

import com.a3v1k.flightSchool.application.game.KillcamManager;
import com.a3v1k.flightSchool.application.scheduler.Scheduler;
import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import lombok.RequiredArgsConstructor;
import me.jumper251.replay.api.ReplayAPI;
import me.jumper251.replay.api.ReplaySessionFinishEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Paper implementation of {@link KillcamManager}.
 *
 * <p>Manages rolling AdvancedReplay recordings per player and plays back the
 * 10-second window before death as a killcam. Listens to {@link ReplaySessionFinishEvent}
 * to respawn the player after the killcam ends.</p>
 *
 * <p>Listener registration is performed by {@link FlightSchool} — this class does NOT
 * self-register. Construct with the plugin instance and an injected {@link Scheduler}.</p>
 */
@RequiredArgsConstructor
public final class PaperKillcamManager implements KillcamManager, Listener {

    private static final int WINDOW_SECONDS = 10;

    private final FlightSchool plugin;
    private final Scheduler scheduler;

    private final Map<UUID, String> activeReplayNames = new HashMap<>();
    private final Map<UUID, String> pendingRespawns = new HashMap<>();
    private final Map<UUID, Scheduler.Task> recordingTasks = new HashMap<>();

    /**
     * Start continuous rolling recording for player.
     */
    @Override
    public void startRecording(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        String replayName = "live_" + uuid;

        Scheduler.Task existingTask = recordingTasks.remove(uuid);
        if (existingTask != null) {
            existingTask.cancel();
        }

        String previousReplayName = activeReplayNames.remove(uuid);
        if (previousReplayName != null) {
            ReplayAPI.getInstance().stopReplay(previousReplayName, false);
        }

        activeReplayNames.put(uuid, replayName);

        startNewRecording(player, replayName);

        // Restart recording every 10 seconds to simulate rolling buffer
        Scheduler.Task recordingTask = scheduler.runRepeating(t -> {
            if (!player.isOnline()) {
                recordingTasks.remove(uuid);
                t.cancel();
                return;
            }

            if (!activeReplayNames.containsKey(uuid)) {
                recordingTasks.remove(uuid);
                t.cancel();
                return;
            }

            // Stop old recording (save)
            ReplayAPI.getInstance().stopReplay(replayName, false, true);

            // Start new one
            startNewRecording(player, replayName);
        }, WINDOW_SECONDS * 20L, WINDOW_SECONDS * 20L);

        recordingTasks.put(uuid, recordingTask);
    }

    private void startNewRecording(Player player, String replayName) {
        ReplayAPI.getInstance().recordReplay(replayName, player);
    }

    /**
     * Called on plane death.
     */
    @Override
    public void playKillcam(Player deadPlayer, String teamName) {
        UUID uuid = deadPlayer.getUniqueId();
        String replayName = activeReplayNames.get(uuid);

        if (replayName == null) return;

        // Stop recording and save last window
        ReplayAPI.getInstance().stopReplay(replayName, true, true);

        deadPlayer.setGameMode(GameMode.SPECTATOR);

        pendingRespawns.put(uuid, teamName);

        // Small delay ensures replay file finishes saving
        scheduler.runLater(() -> {
            ReplayAPI.getInstance().playReplay(replayName, deadPlayer);
        }, 10L);
    }

    /**
     * When replay ends → respawn plane and resume recording.
     */
    @EventHandler
    public void onReplayFinished(ReplaySessionFinishEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!pendingRespawns.containsKey(uuid)) return;

        String teamName = pendingRespawns.remove(uuid);

        player.setGameMode(GameMode.SURVIVAL);

        plugin.getGameOrchestrator().spawnDelayedPlane(
            teamName,
            plugin.getConfigManager().getPlaneLocations().get(teamName).getFirst(),
            player,
            0
        );

        // Restart rolling recording
        startRecording(player);
    }

    /**
     * Optional cleanup.
     */
    @Override
    public void stopRecording(Player player) {
        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Scheduler.Task recordingTask = recordingTasks.remove(uuid);
        if (recordingTask != null) {
            recordingTask.cancel();
        }
        String replayName = activeReplayNames.remove(uuid);

        if (replayName != null) {
            ReplayAPI.getInstance().stopReplay(replayName, false);
        }
    }

    @Override
    public void reset() {
        for (UUID uuid : activeReplayNames.keySet().stream().toList()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                stopRecording(player);
            } else {
                String replayName = activeReplayNames.remove(uuid);
                if (replayName != null) {
                    ReplayAPI.getInstance().stopReplay(replayName, false);
                }
            }
        }

        for (Scheduler.Task recordingTask : recordingTasks.values()) {
            recordingTask.cancel();
        }
        recordingTasks.clear();
        pendingRespawns.clear();
    }
}
