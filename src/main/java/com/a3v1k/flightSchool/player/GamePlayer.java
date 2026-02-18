package com.a3v1k.flightSchool.player;

import com.a3v1k.flightSchool.team.Team;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GamePlayer {

    private final UUID uuid;
    private Team team;
    private Role role;
    private boolean lastStand = true;

    public GamePlayer(Player player) {
        this.uuid = player.getUniqueId();
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean getLastStand() {
        return this.lastStand;
    }

    public void setLastStand(boolean lastStand) {
        this.lastStand = lastStand;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
