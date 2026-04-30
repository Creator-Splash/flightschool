package com.a3v1k.flightSchool.application.game;

import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import com.a3v1k.flightSchool.domain.team.Team;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ScoreManager {

    private final FlightSchool plugin;
    private final Map<UUID, Integer> playerScores = new HashMap<>();
    private final Map<Team, Integer> teamScores = new HashMap<>();

    public ScoreManager() {
        this.plugin = FlightSchool.getInstance();
    }

    public void addScore(Player player, int score) {
        playerScores.put(player.getUniqueId(), getScore(player) + score);
        Team team = plugin.getGameManager().getGamePlayer(player.getUniqueId()).getTeam();
        if (team != null) {
            addScore(team, score);
        }
    }

    public void addScore(Team team, int score) {
        teamScores.put(team, getScore(team) + score);
    }

    public int getScore(Player player) {
        return playerScores.getOrDefault(player.getUniqueId(), 0);
    }

    public int getScore(Team team) {
        return teamScores.getOrDefault(team, 0);
    }
}
