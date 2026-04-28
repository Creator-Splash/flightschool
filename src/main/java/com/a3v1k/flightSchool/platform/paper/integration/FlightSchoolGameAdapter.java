package com.a3v1k.flightSchool.platform.paper.integration;

import com.a3v1k.flightSchool.application.game.GameManager;
import com.a3v1k.flightSchool.application.team.TeamManager;
import com.a3v1k.flightSchool.domain.match.GameState;
import com.a3v1k.flightSchool.domain.team.Team;
import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import creatorsplash.creatorsplashcore.api.EndCondition;
import creatorsplash.creatorsplashcore.api.GameAdapter;
import creatorsplash.creatorsplashcore.api.GameRequirements;
import creatorsplash.creatorsplashcore.api.ProxyConnector;
import creatorsplash.creatorsplashcore.api.TeamMode;
import creatorsplash.creatorsplashcore.event.GameContext;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class FlightSchoolGameAdapter implements GameAdapter {

    private static final long EVACUATION_DELAY_TICKS = 40L;

    private static final Map<String, String> EVENT_TO_FS_TEAM = Map.of(
            "orca", "a",
            "seahorse", "b",
            "turtle", "c",
            "dolphin", "d",
            "swordfish", "e",
            "stingray", "f",
            "jellyfish", "g",
            "octopus", "h");

    private final FlightSchool plugin;
    private final String proxyLobbyServer;

    public FlightSchoolGameAdapter(FlightSchool plugin) {
        this.plugin = plugin;
        this.proxyLobbyServer = plugin.getConfig()
                .getString("event-mode.proxy-lobby-server", "creatorsplash");
    }

    @Override
    public String gameId() {
        return "flightschool";
    }

    @Override
    public String displayName() {
        return "Flight School";
    }

    @Override
    public GameRequirements requirements() {
        return GameRequirements.builder()
                .teamMode(TeamMode.TEAM_VS_TEAM)
                .teamRange(2, 8)
                .playersPerTeamRange(1, 5)
                .endCondition(EndCondition.LAST_TEAM_STANDING)
                .build();
    }

    @Override
    public void onGameStart(GameContext ctx) {
        GameManager gm = plugin.getGameManager();
        if (gm.getGameState() != GameState.LOBBY) {
            plugin.getLogger().info("[FlightSchoolGameAdapter] State was "
                    + gm.getGameState() + "; resetting before round start.");
            try {
                gm.resetRoundState();
            } catch (Throwable t) {
                plugin.getLogger().warning("[FlightSchoolGameAdapter] resetRoundState threw: " + t.getMessage());
            }
        }

        seedTeamsFromContext(ctx);

        try {
            plugin.startGame();
            plugin.getLogger().info("[FlightSchoolGameAdapter] Round "
                    + ctx.round() + "/" + ctx.totalRounds() + " started for "
                    + ctx.teamAssignments().size() + " team(s).");
        } catch (Throwable ex) {
            String reason = ex.getClass().getSimpleName() + ": "
                    + (ex.getMessage() == null ? "" : ex.getMessage());
            plugin.getLogger().warning("[FlightSchoolGameAdapter] startGame threw: " + reason);
            publishStartFailed(ctx, reason);
        }
    }

    @Override
    public void onPlayerArrive(Player player, String teamName, GameContext ctx) {
        Team fsTeam = resolveFsTeam(teamName);
        if (fsTeam == null) {
            plugin.getLogger().warning("[FlightSchoolGameAdapter] Player "
                    + player.getName() + " arrived with no resolvable team (event team='"
                    + teamName + "').");
            return;
        }

        plugin.getGameManager().assignPlayerToTeam(player, fsTeam);

        TeamManager tm = plugin.getTeamManager();
        if (tm != null) {
            tm.teleportPlayerToSpawn(player, fsTeam);
        }

        GameState state = plugin.getGameManager().getGameState();
        if (state == GameState.IN_GAME || state == GameState.CINEMATIC) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                player.setGameMode(GameMode.SURVIVAL);
                player.getInventory().clear();
                player.setFoodLevel(20);
                player.setSaturation(20f);
                AttributeInstance maxHp = player.getAttribute(Attribute.MAX_HEALTH);
                player.setHealth(maxHp != null ? maxHp.getValue() : 20.0);
                player.clearActivePotionEffects();
            });
        }
    }

    @Override
    public void onGameEnd(GameContext ctx) {
        try {
            plugin.stopGame();
        } catch (Throwable ex) {
            plugin.getLogger().warning("[FlightSchoolGameAdapter] stopGame threw: " + ex.getMessage());
        }
        scheduleEvacuation();
    }

    private void seedTeamsFromContext(GameContext ctx) {
        GameManager gm = plugin.getGameManager();
        for (Map.Entry<String, List<UUID>> entry : ctx.teamAssignments().entrySet()) {
            Team fsTeam = resolveFsTeam(entry.getKey());
            if (fsTeam == null) {
                plugin.getLogger().warning("[FlightSchoolGameAdapter] No FS team mapping for event team '"
                        + entry.getKey() + "'; skipping its " + entry.getValue().size() + " player(s).");
                continue;
            }
            for (UUID id : entry.getValue()) {
                Player p = Bukkit.getPlayer(id);
                if (p == null) continue;
                gm.assignPlayerToTeam(p, fsTeam);
            }
        }
    }

    private Team resolveFsTeam(String eventTeamName) {
        if (eventTeamName == null || eventTeamName.isBlank()) return null;
        String fsId = EVENT_TO_FS_TEAM.get(eventTeamName.toLowerCase(Locale.ROOT).trim());
        if (fsId == null) return null;
        return plugin.getGameManager().getTeam(fsId);
    }

    private void publishStartFailed(GameContext ctx, String reason) {
        try {
            //ProxyConnector.getInstance().notifyGameStartFailed(ctx.eventId(), gameId(), reason);
            // TODO RECONNECT THIS
        } catch (Throwable t) {
            plugin.getLogger().warning(
                    "[FlightSchoolGameAdapter] Could not publish GAME_START_FAILED: " + t.getMessage());
        }
    }

    private void scheduleEvacuation() {
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                creatorsplash.creatorsplashcore.api.CreatorSplashCore.evacuateToLobby(
                        plugin, Bukkit.getOnlinePlayers(), proxyLobbyServer),
                EVACUATION_DELAY_TICKS);
    }
}
