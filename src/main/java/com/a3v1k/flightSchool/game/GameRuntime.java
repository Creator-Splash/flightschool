package com.a3v1k.flightSchool.game;

import com.a3v1k.flightSchool.blimp.BlimpData;
import com.a3v1k.flightSchool.player.GamePlayer;
import com.a3v1k.flightSchool.player.Role;
import com.a3v1k.flightSchool.team.Team;
import io.lumine.mythic.core.mobs.ActiveMob;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
public class GameRuntime {

    @Setter
    private GameState gameState = GameState.LOBBY;
    private final Map<UUID, GamePlayer> players = new HashMap<>();
    private final Map<String, Team> teams = new HashMap<>();
    private final Map<Team, List<Player>> playerPlaneMaps = new HashMap<>();
    private final ScoreManager scoreManager = new ScoreManager();
    private final Map<String, BlimpData> blimps = new HashMap<>();
    @Setter
    private Map<Team, List<ActiveMob>> teamPlaneMaps = new HashMap<>();
    @Setter
    private Map<String, BlimpHealthManager> healthManagers = new HashMap<>();
    @Setter
    private long gameStartedAt = -1L;

    public void addPlayer(Player player) {
        players.put(player.getUniqueId(), new GamePlayer(player));
    }

    public void removePlayer(Player player) {
        players.remove(player.getUniqueId());
    }

    public GamePlayer getGamePlayer(Player player) {
        return player == null ? null : players.computeIfAbsent(player.getUniqueId(), k -> new GamePlayer(player));
    }

    public void addTeam(Team team) {
        teams.put(team.getName(), team);
    }

    public Team getTeam(String name) {
        return teams.get(name);
    }

    public void assignRole(Player player, Role role) {
        GamePlayer gamePlayer = getGamePlayer(player);
        if (gamePlayer != null) {
            gamePlayer.setRole(role);
        }
    }

    public void registerBlimp(String teamName, List<Location> solidBlocks) {
        blimps.put(teamName, new BlimpData(teamName, solidBlocks));
    }

    public BlimpData getBlimp(String teamName) {
        return blimps.get(teamName);
    }

    public boolean hasBlimp(String teamName) {
        return blimps.containsKey(teamName);
    }

    public long getRoleCount(Team team, Role role) {
        return players.values().stream()
                .filter(player -> player.getTeam() == team && player.getRole() == role)
                .count();
    }

}
