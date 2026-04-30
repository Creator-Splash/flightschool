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
    }

    /* == Orchestrator - Reset == */

    @Override
    public void resetRoundState() {
        if (airspaceManager != null) {
            airspaceManager.shutdown();
            airspaceManager = null;
        }

        if (planeCollisionManager != null) {
            planeCollisionManager.stop();
            planeCollisionManager = null;
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