package com.a3v1k.flightSchool.application.game;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public interface GameOrchestrator {

    void startGame(List<Player> playerList);

    void resetRoundState();

    void explodeBlimp(String teamName);

    void spawnDelayedPlane(
        String teamName,
        Location location,
        Player player,
        int delay
    );

    boolean pasteMap(Location location, int teamCount);

}
