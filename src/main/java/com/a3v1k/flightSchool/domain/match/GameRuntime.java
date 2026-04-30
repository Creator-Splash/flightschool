package com.a3v1k.flightSchool.domain.match;

import com.a3v1k.flightSchool.platform.paper.game.blimp.PaperBlimpHealthManager;
import com.a3v1k.flightSchool.application.game.ScoreManager;
import com.a3v1k.flightSchool.domain.blimp.BlimpData;
import com.a3v1k.flightSchool.domain.player.GamePlayer;
import com.a3v1k.flightSchool.domain.player.Role;
import com.a3v1k.flightSchool.domain.team.Team;
import io.lumine.mythic.core.mobs.ActiveMob;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

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
    private final Map<Team, List<Player>> playerPlaneMaps = new HashMap<>(); // todo maybe move away from full bukkit
    private final ScoreManager scoreManager = new ScoreManager();
    private final Map<String, BlimpData> blimps = new HashMap<>();
    @Setter
    private Map<Team, List<ActiveMob>> teamPlaneMaps = new HashMap<>();
    @Setter
    private Map<String, PaperBlimpHealthManager> healthManagers = new HashMap<>();
    @Setter
    private long gameStartedAt = -1L;

    public void addPlayer(UUID playerId) {
        players.put(playerId, new GamePlayer(playerId));
    }

    public void removePlayer(UUID playerId) {
        players.remove(playerId);
    }

    public GamePlayer getGamePlayer(@Nullable UUID playerId) {
        return playerId == null ? null : players.computeIfAbsent(playerId, k ->
            new GamePlayer(playerId));
    }

    public void addTeam(Team team) {
        teams.put(team.getName(), team);
    }

    public Team getTeam(String name) {
        return teams.get(name);
    }

    public void assignRole(UUID playerId, Role role) {
        GamePlayer gamePlayer = getGamePlayer(playerId);
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
