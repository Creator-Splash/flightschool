package com.a3v1k.flightSchool.team;

import com.a3v1k.flightSchool.FlightSchool;
import com.a3v1k.flightSchool.player.GamePlayer;
import com.a3v1k.flightSchool.player.Role;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Team {

    private final String name;
    private final Color color;
    private final String spawnRegionName;
    private final List<UUID> members = new ArrayList<>();
    private Location blimpSpawnLocation;
    private int cannonCount = 2;
    private boolean blimpDestroyed = false;
    private int destroyedBlimps = 0;

    public Team(String name, Color color, String spawnRegionName) {
        this.name = name;
        this.color = color;
        this.spawnRegionName = spawnRegionName;
    }

    public boolean getBlimpDestroyed() {
        return this.blimpDestroyed;
    }
    public void increaseDestroyedBlimps() {
        this.destroyedBlimps++;
    }

    public int getDestroyedBlimps() {
        return this.destroyedBlimps;
    }

    public void decreaseCannonCount() {
        cannonCount--;
    }

    public int getCannonCount() {
        return cannonCount;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }

    public String getSpawnRegionName() {
        return spawnRegionName;
    }

    public List<Player> getCannonMembers() {
        List<Player> players = new ArrayList<>();
        for(UUID uuid : this.getMembers()) {
            Player player = Bukkit.getPlayer(uuid);
            GamePlayer gamePlayer = FlightSchool.getInstance().getGameManager().getGamePlayer(player);
            if(gamePlayer.getRole() == Role.CANNON_OPERATOR) {
                players.add(player);
            }
        }

        return players;
    }

    public List<Player> getPlaneMembers() {
        List<Player> players = new ArrayList<>();
        for(UUID uuid : this.getMembers()) {
            Player player = Bukkit.getPlayer(uuid);
            GamePlayer gamePlayer = FlightSchool.getInstance().getGameManager().getGamePlayer(player);
            if(gamePlayer.getRole() == Role.PLANE_PILOT) {
                players.add(player);
            }
        }

        return players;
    }

    public List<UUID> getMembers() {
        return members;
    }

    public void addMember(Player player) {
        members.add(player.getUniqueId());
    }

    public void removeMember(Player player) {
        members.remove(player.getUniqueId());
    }

    public Location getBlimpSpawnLocation() {
        return blimpSpawnLocation;
    }

    public void setBlimpSpawnLocation(Location blimpSpawnLocation) {
        this.blimpSpawnLocation = blimpSpawnLocation;
    }

    public boolean isBlimpDestroyed() {
        return blimpDestroyed;
    }

    public void setBlimpDestroyed(boolean blimpDestroyed) {
        this.blimpDestroyed = blimpDestroyed;
    }
}
