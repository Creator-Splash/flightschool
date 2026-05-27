package com.a3v1k.flightSchool.application.game;

import com.a3v1k.flightSchool.domain.blimp.BlimpData;
import com.a3v1k.flightSchool.domain.match.GameState;
import com.a3v1k.flightSchool.domain.player.GamePlayer;
import com.a3v1k.flightSchool.domain.player.Role;
import com.a3v1k.flightSchool.domain.team.Team;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface GameManager {

    /* == State == */
    GameState getGameState();
    void setGameState(GameState gameState);
    long getGameStartedAt();
    void setGameStartedAt(long gameStartedAt);

    /* == Players == */
    void addPlayer(UUID player);
    void removePlayer(UUID player);
    GamePlayer getGamePlayer(UUID player);
    Map<UUID, GamePlayer> getPlayers();

    /* == Teams == */
    void addTeam(Team team);
    Team getTeam(String name);
    Map<String, Team> getTeams();
    void assignPlayerToTeam(UUID player, Team team);
    List<Player> getCannonMembers(Team team);
    List<Player> getPlaneMembers(Team team);

    /* == Roles == */
    void assignRole(UUID player, Role role);
    int getRoleLimit(Role role);
    long getRoleCount(Team team, Role role);
    boolean canAssignRole(Team team, Role role);

    /* == Blimps == */
    void registerBlimp(String teamName, List<Location> solidBlocks);
    BlimpData getBlimp(String teamName);
    boolean hasBlimp(String teamName);
    boolean teamHasAliveTurret(Team team);

    /* == Health == */
    Map<String, BlimpHealthManager> getHealthManager();

    /* == Score == */
    ScoreManager getScoreManager();

}
