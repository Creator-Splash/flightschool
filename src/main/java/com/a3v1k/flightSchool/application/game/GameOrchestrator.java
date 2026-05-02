package com.a3v1k.flightSchool.application.game;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public interface GameOrchestrator {

    void startGame(List<Player> playerList);

    void resetRoundState();

    /**
     * End the current match: cancels the match-end timer, determines a winner,
     * announces it, then resets round state after a short delay.
     *
     * <p>Triggered either by the match timer expiring or by last-team-standing
     * (e.g. when a team's elimination leaves only one alive). No-ops if the game
     * is not currently {@code IN_GAME}.</p>
     */
    void triggerMatchEnd();

    void explodeBlimp(String teamName);

    void spawnDelayedPlane(
        String teamName,
        Location location,
        Player player,
        int delay
    );

    boolean pasteMap(Location location, int teamCount);

}
