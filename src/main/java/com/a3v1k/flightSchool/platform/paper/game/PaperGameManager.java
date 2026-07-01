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
import org.bukkit.GameMode;
import org.bukkit.GameRule;
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
    /**
     * Idempotency latch for {@link #triggerMatchEnd()}. Set to true on the first
     * call and reset to false at the start of {@link #resetRoundState()}.
     * Without this, multiple trigger paths firing inside the
     * {@link #MATCH_END_RESET_DELAY_TICKS} window (state stays IN_GAME until
     * reset runs) would re-announce the winner and stack reset tasks.
     */
    private boolean matchEnding = false;

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
                && p.isOnline()
                && (p.getGameMode() == GameMode.ADVENTURE
                    || p.getGameMode() == GameMode.SURVIVAL)
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
                && p.isOnline()
                && (p.getGameMode() == GameMode.ADVENTURE
                    || p.getGameMode() == GameMode.SURVIVAL)
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
        if (getRoleCount(team, role) >= getRoleLimit(role)) {
            return false;
        }
        // A cannon-only team is stationary and can never attack, so every team must
        // keep at least one plane: cap cannons at teamSize - 1 (forces a solo player
        // onto a plane).
        if (role == Role.CANNON_OPERATOR) {
            return getRoleCount(team, Role.CANNON_OPERATOR) < team.getMembers().size() - 1;
        }
        return true;
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
        // Defensive filter — caller is expected to have filtered already, but we log
        // and drop any players who are offline or in a non-playable gamemode before
        // processing. The load-bearing filter is in getCannonMembers/getPlaneMembers
        // (which feed PlaneSpawnService); this is just the upstream safety net.
        List<Player> activePlayers = playerList.stream()
            .filter(p -> p != null
                && p.isOnline()
                && (p.getGameMode() == GameMode.ADVENTURE
                    || p.getGameMode() == GameMode.SURVIVAL))
            .toList();
        if (activePlayers.size() != playerList.size()) {
            logger.warning("[startGame] Filtered "
                + (playerList.size() - activePlayers.size())
                + " inactive player(s) from incoming list (offline, spectating, or creative).");
        }

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
        setupWorldEnvironment(world);
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
        // Clear match-end latch first so a new round can be ended again.
        matchEnding = false;

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
        if (matchEnding) return;
        matchEnding = true;

        if (matchEndTask != null) {
            matchEndTask.cancel();
            matchEndTask = null;
        }

        Team winner = determineWinner();
        announceWinner(winner);

        scheduler.runLater(() -> {
            // Teleport every online player to the lobby spawn BEFORE resetRoundState
            // despawns the cannon/plane mobs they're mounted on. Otherwise they'd
            // dismount mid-air and fall to their death when the mobs vanish.
            Location lobbySpawn = plugin.getLobbySpawnLocation();
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.teleport(lobbySpawn);
            }
            resetRoundState();
        }, MATCH_END_RESET_DELAY_TICKS);
    }

    @Override
    public void checkLastTeamStanding() {
        long activeTeams = runtime.getTeams().values().stream()
            .filter(this::hasActivePlayer)
            .count();
        if (activeTeams <= 1) {
            triggerMatchEnd();
        }
    }

    private boolean hasActivePlayer(Team team) {
        if (team.getMembers().isEmpty()) return false;
        for (UUID memberId : team.getMembers()) {
            GamePlayer gp = runtime.getGamePlayer(memberId);
            if (gp != null && !gp.isEliminated()) return true;
        }
        return false;
    }

    private Team determineWinner() {
        // Must match checkLastTeamStanding's predicate: a team is in-the-running
        // iff it still has a non-eliminated player. Using teamHasAliveTurret here
        // let unseeded teams (members empty, destroyedBlimps=0) pass as "alive",
        // which collapsed every result into a 0-HP/0-score tie and silently
        // returned the first hash-iterated team (Orca).
        List<Team> aliveTeams = runtime.getTeams().values().stream()
            .filter(this::hasActivePlayer)
            .toList();

        if (aliveTeams.size() == 1) return aliveTeams.getFirst();

        // Multi-team timer expiry, or simultaneous wipe. Restrict the fallback
        // pool to teams that actually had members so phantom unseeded teams
        // can't win on ties. Tiebreakers: blimp HP, team score, team name.
        List<Team> candidates = aliveTeams.isEmpty()
            ? runtime.getTeams().values().stream()
                .filter(t -> !t.getMembers().isEmpty())
                .toList()
            : aliveTeams;

        if (candidates.isEmpty()) return null;

        return candidates.stream()
            .max(Comparator
                .<Team>comparingDouble(t -> {
                    BlimpHealthManager hm = healthManagers.get(t.getName());
                    return hm != null ? hm.getHealth() : 0.0;
                })
                .thenComparingInt(t -> runtime.getScoreManager().getScore(t))
                .thenComparing(Team::getName))
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
            subtitle = Component.text(winner.getDisplayName().toUpperCase(), NamedTextColor.GREEN);
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
            : Component.text("Match ended — winner: " + winner.getDisplayName(), NamedTextColor.GOLD);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(adventureTitle);
            p.sendMessage(chatMessage);
        }
    }

    /* == Orchestrator - spawnDelayedPlane == */

    @Override
    public void spawnDelayedPlane(String teamName, Location location, Player player, int delay) {
        Team team = getTeam(teamName);

        GamePlayer pilotGp = getGamePlayer(player.getUniqueId());
        if (pilotGp != null && pilotGp.isLastStand()) {
            // Blimp HP hit zero — pilot is on last stand, next death is permanent.
            player.showTitle(Title.title(
                Component.text("Eliminated!", NamedTextColor.RED),
                Component.text("Your blimp has been destroyed."),
                Title.Times.times(
                    Duration.ofMillis(500),
                    Duration.ofMillis(3500),
                    Duration.ofMillis(1000)
                )
            ));
            pilotGp.setEliminated(true);
            checkLastTeamStanding();
            return;
        }

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

            // Plane can no longer respawn — mark this player permanently eliminated,
            // then re-check whether only one team remains with active players.
            GamePlayer gp = getGamePlayer(player.getUniqueId());
            if (gp != null) gp.setEliminated(true);
            checkLastTeamStanding();

            // Turret-based fallback: covers cases where the player-eliminated bookkeeping
            // misses (e.g. a player rejoined after team elimination got a fresh GamePlayer
            // with eliminated=false). triggerMatchEnd is idempotent and no-ops outside
            // IN_GAME.
            long teamsWithAliveTurrets = runtime.getTeams().values().stream()
                .filter(this::teamHasAliveTurret)
                .count();
            if (teamsWithAliveTurrets <= 1) {
                triggerMatchEnd();
            }
            return;
        }

        scheduler.runLater(() -> {
            // Respawn is happening — pilot is alive again. Reset the eliminated flag
            // that was set in Vehicle.handlePlaneDeath so subsequent checkLastTeamStanding
            // calls correctly count this player as active.
            GamePlayer gp = getGamePlayer(player.getUniqueId());
            if (gp != null) gp.setEliminated(false);

            planeSpawnService.spawnPlane(teamName, location, player);
            // Defensive recheck in case any other team was eliminated while this
            // respawn was scheduled.
            checkLastTeamStanding();
        }, delay * 20L);
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

    private static final long FIXED_WORLD_TIME_TICKS = 1000L;

    private void setupWorldEnvironment(World world) {
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, Boolean.FALSE);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, Boolean.FALSE);
        world.setStorm(false);
        world.setThundering(false);
        world.setWeatherDuration(0);
        world.setClearWeatherDuration(Integer.MAX_VALUE);
        world.setTime(FIXED_WORLD_TIME_TICKS);
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