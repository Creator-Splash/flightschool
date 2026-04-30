package com.a3v1k.flightSchool.domain.team;

import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import com.a3v1k.flightSchool.domain.player.GamePlayer;
import com.a3v1k.flightSchool.domain.player.Role;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter @Setter
@RequiredArgsConstructor
public class Team {

    private final String displayName;
    private final String name;
    private final Color color;
    private final String spawnRegionName;
    private final List<UUID> members = new ArrayList<>();
    private Location blimpSpawnLocation;
    private int cannonCount = 2;
    private boolean blimpDestroyed = false;
    private int destroyedBlimps = 0;

    public boolean getBlimpDestroyed() {
        return this.blimpDestroyed;
    }
    public void increaseDestroyedBlimps() {
        this.destroyedBlimps++;
    }

    public void decreaseCannonCount() {
        cannonCount--;
    }

    public void addMember(UUID playerId) {
        if (!members.contains(playerId)) {
            members.add(playerId);
        }
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    public void resetRoundState() {
        members.clear();
        blimpSpawnLocation = null;
        cannonCount = 2;
        blimpDestroyed = false;
        destroyedBlimps = 0;
    }

}
