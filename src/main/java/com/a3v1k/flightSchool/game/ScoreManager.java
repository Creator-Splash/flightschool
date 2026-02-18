package com.a3v1k.flightSchool.game;

import com.a3v1k.flightSchool.FlightSchool;
import com.a3v1k.flightSchool.team.Team;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreManager {

    private final FlightSchool plugin;
    private final Map<UUID, Integer> playerScores = new HashMap<>();
    private final Map<Team, Integer> teamScores = new HashMap<>();

    public ScoreManager(FlightSchool plugin) {
        this.plugin = plugin;
    }

    public void addScore(Player player, int score) {
        playerScores.put(player.getUniqueId(), getScore(player) + score);
        Team team = plugin.getGameManager().getGamePlayer(player).getTeam();
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
