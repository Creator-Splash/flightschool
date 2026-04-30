package com.a3v1k.flightSchool.application.game;

import java.util.List;
import java.util.UUID;

public interface LobbyManager {

    void startCinematic(List<UUID> players);

    void startRoleSelection(List<UUID> players);

}
