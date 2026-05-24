package com.a3v1k.flightSchool.platform.paper.integration;

import com.a3v1k.flightSchool.application.game.GameManager;
import com.a3v1k.flightSchool.application.team.TeamManager;
import com.a3v1k.flightSchool.domain.match.GameState;
import com.a3v1k.flightSchool.domain.team.Team;
import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import creatorsplash.creatorsplashcore.api.runtime.GameRuntime;
import creatorsplash.creatorsplashcore.api.runtime.RuntimeServices;
import creatorsplash.creatorsplashcore.api.scoring.EndReason;
import creatorsplash.creatorsplashcore.api.session.MatchSession;
import creatorsplash.creatorsplashcore.event.GameContext;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FlightSchoolRuntime extends GameRuntime {

    private static final Map<String, String> EVENT_TO_FS_TEAM = Map.of(
            "orca", "red",
            "seahorse", "yellow",
            "turtle", "green",
            "dolphin", "blue",
            "swordfish", "dark_blue",
            "stingray", "dark_violet",
            "jellyfish", "violet",
            "octopus", "orange");

    private final FlightSchool plugin;
    private final FlightSchoolGameAdapter adapter;
    private MatchSession session;
    private final Set<Team> seededTeams = new HashSet<>();

    public FlightSchoolRuntime(FlightSchool plugin, FlightSchoolGameAdapter adapter,
            GameContext ctx, RuntimeServices services) {
        super(ctx, services);
        this.plugin = plugin;
        this.adapter = adapter;
    }

    @Override
    protected boolean wantsPreGamePhase() {
        return true;
    }

    @Override
    protected void onStart() {
        GameManager gm = plugin.getGameManager();
        if (gm.getGameState() != GameState.LOBBY) {
            plugin.getLogger().info("[FlightSchoolRuntime] State was "
                    + gm.getGameState() + "; resetting before round start.");
            try { gm.resetRoundState(); } catch (Throwable t) {
                plugin.getLogger().warning("[FlightSchoolRuntime] resetRoundState threw: " + t.getMessage());
            }
        }

        session = services().sessions().create(0, null);
        seedTeamsFromContext(context());
        plugin.getLogger().info("[FlightSchoolRuntime] Round " + context().round() + "/"
                + context().totalRounds() + " prepared with " + seededTeams.size()
                + " team(s); waiting for first arrival before launching cinematic.");
    }

    @Override
    protected void onGameplayStart() {
        try {
            plugin.startGame();
            plugin.getLogger().info("[FlightSchoolRuntime] Game " + context().round()
                    + "/" + context().totalRounds() + " launched (PRE_GAME -> PLAYING).");
        } catch (Throwable ex) {
            String reason = ex.getClass().getSimpleName() + ": "
                    + (ex.getMessage() == null ? "" : ex.getMessage());
            plugin.getLogger().warning("[FlightSchoolRuntime] startGame threw: " + reason);
            proxy().notifyGameStartFailed(context().eventId(), adapter.gameId(), reason);
            requestEnd(EndReason.INTERNAL_ERROR);
        }
    }

    @Override
    protected void onPlayerArrive(Player player, @Nullable String teamName) {
        Team fsTeam = resolveFsTeam(teamName);
        if (fsTeam == null) {
            plugin.getLogger().warning("[FlightSchoolRuntime] Player " + player.getName()
                    + " arrived with no resolvable team (event team='" + teamName + "').");
            return;
        }

        plugin.getGameManager().assignPlayerToTeam(player, fsTeam);
        if (session != null && teamName != null && !teamName.isBlank()) {
            session.addPlayer(player.getUniqueId(), teamName.toLowerCase(),
                    player.getName(), false);
        }

        TeamManager tm = plugin.getTeamManager();
        if (tm != null) tm.teleportPlayerToSpawn(player, fsTeam);

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
    protected void onEnd(EndReason reason) {
        if (plugin.getGameManager().getGameState() != GameState.LOBBY) {
            plugin.getLogger().info("[FlightSchoolRuntime] CSC signalled game end (" + reason
                    + "); stopping active game.");
            try { plugin.stopGame(); } catch (Throwable t) {
                plugin.getLogger().warning("[FlightSchoolRuntime] stopGame threw: " + t.getMessage());
            }
        }
    }

    @Override
    protected void onCleanup() {
        adapter.clearActiveRuntime(this);
    }

    @Override
    @Nullable
    protected EndReason evaluateEndCondition() {
        GameManager gm = plugin.getGameManager();
        if (gm.getGameState() == GameState.ENDING) return EndReason.NATURAL_WIN;
        if (gm.getGameState() != GameState.IN_GAME) return null;
        if (seededTeams.size() < 2) return null;
        int teamsAlive = 0;
        for (Team t : seededTeams) {
            if (!t.getBlimpDestroyed()) teamsAlive++;
        }
        if (teamsAlive <= 1) return EndReason.NATURAL_WIN;
        return null;
    }

    private void seedTeamsFromContext(GameContext ctx) {
        GameManager gm = plugin.getGameManager();
        seededTeams.clear();
        for (Map.Entry<String, List<UUID>> entry : ctx.teamAssignments().entrySet()) {
            Team fsTeam = resolveFsTeam(entry.getKey());
            if (fsTeam == null) {
                plugin.getLogger().warning("[FlightSchoolRuntime] No FS team mapping for event team '"
                        + entry.getKey() + "'; skipping its " + entry.getValue().size() + " player(s).");
                continue;
            }
            seededTeams.add(fsTeam);
            String side = entry.getKey().toLowerCase(Locale.ROOT);
            for (UUID id : entry.getValue()) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) gm.assignPlayerToTeam(p, fsTeam);
                if (session != null) {
                    session.addPlayer(id, side, p == null ? null : p.getName(), false);
                }
            }
        }
    }

    @Nullable
    private Team resolveFsTeam(@Nullable String eventTeamName) {
        if (eventTeamName == null || eventTeamName.isBlank()) return null;
        String fsId = EVENT_TO_FS_TEAM.get(eventTeamName.toLowerCase(Locale.ROOT).trim());
        if (fsId == null) return null;
        return plugin.getGameManager().getTeam(fsId);
    }
}
