package com.a3v1k.flightSchool.application.game;

import java.util.List;
import java.util.UUID;

public interface LobbyManager {

    void startCinematic(List<UUID> players);

    void startRoleSelection(List<UUID> players);

    /**
     * Cancel any cinematic/role-selection countdown tasks still running. Must be
     * called on force-end; an orphaned countdown would otherwise relaunch the
     * game after the round was already torn down.
     */
    default void cancelPendingLobbyTasks() {}

}
