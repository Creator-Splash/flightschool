package com.a3v1k.flightSchool.application.game;

import org.bukkit.entity.Player;

/**
 * Orchestrates rolling player recordings and killcam playback.
 *
 * <p>Implementations record a continuous rolling buffer per player while alive
 * and play back the most recent window when a player dies. The Paper implementation
 * lives in {@code platform/paper/game/PaperKillcamManager} and integrates with the
 * AdvancedReplay API.</p>
 */
public interface KillcamManager {

    /**
     * Start a rolling recording for the given player. Cancels and restarts any
     * existing recording for the same player.
     */
    void startRecording(Player player);

    /**
     * Stop and discard the rolling recording for the given player. No-op if
     * no recording is active for the player.
     */
    void stopRecording(Player player);

    /**
     * Save the player's current rolling window and play it back as a killcam.
     * The player is placed in spectator mode for the duration of the playback;
     * the implementation handles respawn after playback completes.
     */
    void playKillcam(Player deadPlayer, String teamName);

    /**
     * Cancel all active recordings and clear all pending respawns. Intended for
     * round resets and plugin shutdown.
     */
    void reset();
}
