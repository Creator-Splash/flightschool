package com.a3v1k.flightSchool.domain.player;

import com.a3v1k.flightSchool.domain.team.Team;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@RequiredArgsConstructor
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

}
