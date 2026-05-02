package com.a3v1k.flightSchool.platform.paper.game;

import com.a3v1k.flightSchool.application.game.BlimpHealthManager;
import com.a3v1k.flightSchool.application.game.GameManager;
import com.a3v1k.flightSchool.application.game.GameOrchestrator;
import com.a3v1k.flightSchool.application.game.ScoreManager;
import com.a3v1k.flightSchool.application.scheduler.Scheduler;
import com.a3v1k.flightSchool.domain.blimp.BlimpData;
import com.a3v1k.flightSchool.domain.match.GameRuntime;
import com.a3v1k.flightSchool.domain.match.GameState;
import com.a3v1k.flightSchool.domain.player.GamePlayer;
import com.a3v1k.flightSchool.domain.player.Role;
import com.a3v1k.flightSchool.domain.team.Team;
import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import com.a3v1k.flightSchool.platform.paper.game.blimp.BlimpExplosionService;
import com.a3v1k.flightSchool.platform.paper.game.blimp.BlimpSchematicService;
import com.a3v1k.flightSchool.platform.paper.game.blimp.PaperBlimpHealthManager;
import com.a3v1k.flightSchool.platform.paper.game.plane.PlaneCollisionManager;
import com.a3v1k.flightSchool.platform.paper.game.plane.PlaneSpawnService;
import io.lumine.mythic.core.mobs.ActiveMob;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;

public final class PaperGameManager implements GameManager, GameOrchestrator {

    private static final int ROLE_LIMIT_CANNON = 2;
    private static final int ROLE_LIMIT_PLANE = 3;

    /** Delay between announcing the winner and resetting round state, in ticks. */
    private static final long MATCH_END_RESET_DELAY_TICKS = 100L;

    private final FlightSchool plugin;
    private final Scheduler scheduler;
    private final Logger logger;
    private final Map<Role, Integer> roleLimits;

    private final BlimpSchematicService schematicService;
    private final BlimpExplosionService explosionService;
    private final PlaneSpawnService planeSpawnService;

    private final Map<Team, List<ActiveMob>> teamPlaneMaps = new HashMap<>();
    private final Map<String, PaperBlimpHealthManager> healthManagers = new HashMap<>();
    private final Map<String, Scheduler.Task> blimpHealthTasks = new HashMap<>();

    @Getter
    private GameRuntime runtime;
    private AirspaceManager airspaceManager;
    private PlaneCollisionManager planeCollisionManager;
    private Scheduler.Task matchEndTask;

    public PaperGameManager(
        @NotNull final FlightSchool plugin,
        @NotNull final Scheduler scheduler
    ) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.logger = plugin.getLogger();
        this.runtime = new GameRuntime();
        this.roleLimits = Map.of(
            Role.CANNON_OPERATOR, ROLE_LIMIT_CANNON,
            Role.PLANE_PILOT, ROLE_LIMIT_PLANE
        );

        this.schematicService = new BlimpSchematicService(
            plugin.getDataFolder(), logger, this);

        this.explosionService = new BlimpExplosionService(scheduler, logger);

        this.planeSpawnService = new PlaneSpawnService(
            plugin, this, scheduler, logger,
            teamPlaneMaps, healthManagers, blimpHealthTasks);

        try {
            schematicService.loadSchematic();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load blimp schematic", e);
        }
    }

    /* == Game - State == */

    @Override
    public GameState getGameState() {
        return runtime.getGameState();
    }

    @Override
    public void setGameState(GameState gameState) {
        runtime.setGameState(gameState);
        plugin.enableLocatorBar();
        logger.info("GameState set to: " + gameState);
    }

    @Override
    public long getGameStartedAt() {
        return runtime.getGameStartedAt();
    }

    @Override
    public void setGameStartedAt(long gameStartedAt) {
        runtime.setGameStartedAt(gameStartedAt);
    }

    /* == Game - Players == */

    @Override
    public void addPlayer(UUID player) {
        runtime.addPlayer(player);
    }

    @Override
    public void removePlayer(UUID player) {
        runtime.removePlayer(player);
    }

    @Override
    public GamePlayer getGamePlayer(UUID player) {
        return player == null ? null : runtime.getGamePlayer(player);

    }

    @Override
    public Map<UUID, GamePlayer> getPlayers() {
        return runtime.getPlayers();
    }

    /* == Game - Teams == */

    @Override
    public void addTeam(Team team) {
        runtime.addTeam(team);
    }

    @Override
    public Team getTeam(String name) {
        return runtime.getTeam(name);
    }

    @Override
    public Map<String, Team> getTeams() {
        return runtime.getTeams();
    }

    @Override
    public void assignPlayerToTeam(UUID playerId, Team team) {
        if (playerId == null) return;
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;

        for (Team existing : runtime.getTeams().values()) {
            existing.removeMember(playerId);
        }

        GamePlayer gamePlayer = getGamePlayer(playerId);
        if (gamePlayer == null) return;

        gamePlayer.setTeam(team);

        if (team != null) {
            team.addMember(playerId);
        }

        if (plugin.getTeamVisualManager() != null) {
            plugin.getTeamVisualManager().syncPlayer(player);
        }
    }

    @Override
    public List<Player> getCannonMembers(Team team) {
        return team.getMembers().stream()
            .map(Bukkit::getPlayer)
            .filter(p -> p != null
                && getGamePlayer(p.getUniqueId()) != null
                && getGamePlayer(p.getUniqueId()).getTeam() == team
                && getGamePlayer(p.getUniqueId()).getRole() == Role.CANNON_OPERATOR)
            .toList();
    }

    @Override
    public List<Player> getPlaneMembers(Team team) {
        return team.getMembers().stream()
            .map(Bukkit::getPlayer)
            .filter(p -> p != null
                && getGamePlayer(p.getUniqueId()) != null
                && getGamePlayer(p.getUniqueId()).getTeam() == team
                && getGamePlayer(p.getUniqueId()).getRole() == Role.PLANE_PILOT)
            .toList();
    }

    /* == Game - Role == */

    @Override
    public void assignRole(UUID player, Role role) {
        runtime.assignRole(player, role);
    }

    @Override
    public int getRoleLimit(Role role) {
        return roleLimits.getOrDefault(role, 0);
    }

    @Override
    public long getRoleCount(Team team, Role role) {
        return runtime.getRoleCount(team, role);
    }

    @Override
    public boolean canAssignRole(Team team, Role role) {
        return getRoleCount(team, role) < getRoleLimit(role);
    }

    /* == Game - Blimps == */

    @Override
    public void registerBlimp(String teamName, List<Location> solidBlocks) {
        runtime.registerBlimp(teamName, solidBlocks);
    }

    @Override
    public BlimpData getBlimp(String teamName) {
        return runtime.getBlimp(teamName);
    }

    @Override
    public boolean hasBlimp(String teamName) {
        return runtime.hasBlimp(teamName);
    }

    @Override
    public boolean teamHasAliveTurret(Team team) {
        return team.getDestroyedBlimps() < getRoleLimit(Role.CANNON_OPERATOR);
    }

    /* == Game - Health == */

    @Override
    public Map<String, BlimpHealthManager> getHealthManager() {
        return Collections.unmodifiableMap(healthManagers);
    }

    /* == Game - Score == */

    @Override
    public ScoreManager getScoreManager() {
        return runtime.getScoreManager();
    }

    /* == Orchestrator - Start */

    @Override
    public void startGame(List<Player> playerList) {
        setGameState(GameState.IN_GAME);
        runtime.setGameStartedAt(System.currentTimeMillis());
        plugin.enableLocatorBar();

        Map<String, List<Location>> cannonLocations = plugin.getConfigManager().getCannonLocations();
        Map<String, List<Location>> planeLocations = plugin.getConfigManager().getPlaneLocations();

        teamPlaneMaps.clear();
        healthManagers.clear();

        World world = cannonLocations.values().stream()
            .flatMap(List::stream)
            .findFirst()
            .map(Location::getWorld)
            .orElseThrow(() -> new IllegalStateException("No cannon locations configured"));

        schematicService.computeBlimpData(world, new Location(world, 0, 65, 0));

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Component.text("The game has started. Good luck!", NamedTextColor.GOLD));
            player.getInventory().clear();
        }

        setupWorldBorder(world);
        startAirspaceManager(world, planeLocations);
        startPlaneCollisionManager();

        planeSpawnService.spawnCannons(cannonLocations);
        planeSpawnService.spawnPlanes(planeLocations);

        scheduleMatchEnd();
    }

    private void scheduleMatchEnd() {
        if (matchEndTask != null) {
            matchEndTask.cancel();
        }
        int matchDuration = plugin.getConfigManager().getMatchDuration();
        matchEndTask = scheduler.runLater(this::triggerMatchEnd, matchDuration * 20L);
    }

    /* == Orchestrator - Reset == */

    @Override
    public void resetRoundState() {
        // Snapshot player→team assignments before destruction so we can restore them
        // after the fresh runtime is built. Without this, every reset re-shuffles teams
        // (T7 bug). assignTeams in PaperLobbyManager preserves any team already set, so
        // restoring here is enough to keep players on their teams across stop→start.
        Map<UUID, String> teamSnapshot = new HashMap<>();
        for (Map.Entry<UUID, GamePlayer> entry : runtime.getPlayers().entrySet()) {
            Team t = entry.getValue().getTeam();
            if (t != null) teamSnapshot.put(entry.getKey(), t.getName());
        }

        if (airspaceManager != null) {
            airspaceManager.shutdown();
            airspaceManager = null;
        }

        if (planeCollisionManager != null) {
            planeCollisionManager.stop();
            planeCollisionManager = null;
        }

        if (matchEndTask != null) {
            matchEndTask.cancel();
            matchEndTask = null;
        }

        blimpHealthTasks.values().forEach(Scheduler.Task::cancel);
        blimpHealthTasks.clear();

        plugin.getKillcamManager().reset();

        for (BlimpHealthManager healthManager : healthManagers.values()) {
            if (healthManager != null) healthManager.disableAndDespawn();
        }

        for (List<ActiveMob> activeMobs : teamPlaneMaps.values()) {
            despawnActiveMobs(activeMobs);
        }

        List<Team> teams = new ArrayList<>(runtime.getTeams().values());
        teams.forEach(Team::resetRoundState);

        GameRuntime freshRuntime = new GameRuntime();
        teams.forEach(freshRuntime::addTeam);
        Bukkit.getOnlinePlayers().forEach(p -> freshRuntime.addPlayer(p.getUniqueId()));
        freshRuntime.setGameState(GameState.LOBBY);
        freshRuntime.setGameStartedAt(-1L);
        runtime = freshRuntime;

        // Restore team assignments after the fresh runtime is built. Must happen after
        // Team::resetRoundState (which clears members) and after fresh GamePlayer instances
        // are created — otherwise either step would stomp the restoration.
        for (Map.Entry<UUID, String> entry : teamSnapshot.entrySet()) {
            Team team = freshRuntime.getTeam(entry.getValue());
            if (team == null) continue;
            GamePlayer gp = freshRuntime.getGamePlayer(entry.getKey());
            if (gp == null) continue;
            gp.setTeam(team);
            team.addMember(entry.getKey());
        }

        teamPlaneMaps.clear();
        healthManagers.clear();
        blimpHealthTasks.clear();

        if (plugin.getTeamVisualManager() != null) {
            plugin.getTeamVisualManager().refreshAll();
        }
    }

    /* == Orchestrator - explodeBlimp == */

    @Override
    public void explodeBlimp(String teamName) {
        BlimpData blimp = getBlimp(teamName);
        Team team = getTeam(teamName);
        if (blimp == null || team == null) {
            logger.warning("explodeBlimp called for unknown team: " + teamName);
            return;
        }

        explosionService.explode(blimp, team);
    }

    /* == Orchestrator - triggerMatchEnd == */

    @Override
    public void triggerMatchEnd() {
        if (runtime.getGameState() != GameState.IN_GAME) return;

        if (matchEndTask != null) {
            matchEndTask.cancel();
            matchEndTask = null;
        }

        Team winner = determineWinner();
        announceWinner(winner);

        scheduler.runLater(this::resetRoundState, MATCH_END_RESET_DELAY_TICKS);
    }

    private Team determineWinner() {
        List<Team> aliveTeams = runtime.getTeams().values().stream()
            .filter(this::teamHasAliveTurret)
            .toList();

        // Last team standing — fast path.
        if (aliveTeams.size() == 1) return aliveTeams.getFirst();

        // Multi-team timer expiry, or zero-alive (everyone died simultaneously).
        // Tiebreaker: highest blimp HP, then highest team score (kill-count proxy).
        List<Team> candidates = aliveTeams.isEmpty()
            ? new ArrayList<>(runtime.getTeams().values())
            : aliveTeams;

        return candidates.stream()
            .max(Comparator.<Team>comparingDouble(t -> {
                BlimpHealthManager hm = healthManagers.get(t.getName());
                return hm != null ? hm.getHealth() : 0.0;
            }).thenComparingInt(t -> runtime.getScoreManager().getScore(t)))
            .orElse(null);
    }

    private void announceWinner(Team winner) {
        Component titleText;
        Component subtitle;

        if (winner == null) {
            titleText = Component.text("Match Over", NamedTextColor.GOLD);
            subtitle = Component.text("No winner determined", NamedTextColor.GRAY);
        } else {
            titleText = Component.text("WINNER", NamedTextColor.GOLD);
            subtitle = Component.text(winner.getName().toUpperCase(), NamedTextColor.GREEN);
        }

        Title adventureTitle = Title.title(
            titleText,
            subtitle,
            Title.Times.times(
                Duration.ofMillis(500),
                Duration.ofSeconds(4),
                Duration.ofMillis(1000)
            )
        );

        Component chatMessage = winner == null
            ? Component.text("Match ended — no winner determined.", NamedTextColor.GOLD)
            : Component.text("Match ended — winner: " + winner.getName(), NamedTextColor.GOLD);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(adventureTitle);
            p.sendMessage(chatMessage);
        }
    }

    /* == Orchestrator - spawnDelayedPlane == */

    @Override
    public void spawnDelayedPlane(String teamName, Location location, Player player, int delay) {
        Team team = getTeam(teamName);

        if (team != null && !teamHasAliveTurret(team)) {
            player.showTitle(Title.title(
                Component.text("Eliminated!", NamedTextColor.RED),
                Component.text("Your team's cannons have been destroyed."),
                Title.Times.times(
                    Duration.ofMillis(500),
                    Duration.ofMillis(3500),
                    Duration.ofMillis(1000)
                )
            ));
            return;
        }

        scheduler.runLater(
            () -> planeSpawnService.spawnPlane(teamName, location, player),
            delay * 20L
        );
    }

    /* == Orchestrator - pasteMap == */

    @Override
    public boolean pasteMap(Location location, int teamCount) {
        return schematicService.pasteMap(location, teamCount);
    }

    /* == Platform Accessors == */

    public Map<Team, List<ActiveMob>> getTeamPlanes() {
        return Collections.unmodifiableMap(teamPlaneMaps);
    }

    /* == Private - Helpers == */

    private void setupWorldBorder(World world) {
        world.getWorldBorder().setCenter(world.getSpawnLocation());
        world.getWorldBorder().setSize(2048);
    }

    private void startAirspaceManager(World world, Map<String, List<Location>> planeLocations) {
        int minFlightY = plugin.getConfigManager().getMinFlightY(world, planeLocations);
        int maxFlightY = plugin.getConfigManager().getMaxFlightY(world, planeLocations);

        if (minFlightY >= maxFlightY) minFlightY = maxFlightY - 32;

        if (airspaceManager != null) airspaceManager.shutdown();
        airspaceManager = new AirspaceManager(plugin, this, minFlightY, maxFlightY);
        airspaceManager.start(scheduler);
    }

    private void startPlaneCollisionManager() {
        if (planeCollisionManager != null) planeCollisionManager.stop();
        planeCollisionManager = new PlaneCollisionManager(plugin, this);
        planeCollisionManager.start(scheduler);
    }

    /* == Private - Cleanup == */

    private void despawnActiveMobs(List<ActiveMob> activeMobs) {
        if (activeMobs == null) return;
        for (ActiveMob mob : activeMobs) {
            if (mob == null || mob.getEntity() == null) continue;
            mob.despawn();
        }
    }

}