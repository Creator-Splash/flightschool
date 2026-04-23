package com.a3v1k.flightSchool.domain.player;

import com.a3v1k.flightSchool.domain.team.Team;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GamePlayer {

    @Getter
    private final UUID uuid;
    @Getter @Setter
    private Team team;
    @Getter @Setter
    private Role role;
    @Getter @Setter
    private boolean lastStand = true;
    @Getter @Setter
    private boolean eliminated = false;

    public GamePlayer(Player player) {
        this.uuid = player.getUniqueId();
    }

}
