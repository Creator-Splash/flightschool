package com.a3v1k.flightSchool.platform.paper;

import com.a3v1k.flightSchool.platform.paper.command.FlightSchoolCommand;
import com.a3v1k.flightSchool.platform.paper.command.TempCommands;
import com.a3v1k.flightSchool.application.game.*;
import com.a3v1k.flightSchool.platform.paper.listener.GameListener;
import com.a3v1k.flightSchool.platform.paper.listener.PlayerListener;
import com.a3v1k.flightSchool.platform.paper.listener.TeamListener;
import com.a3v1k.flightSchool.platform.paper.display.placeholder.PointsExpansion;
import com.a3v1k.flightSchool.platform.paper.display.placeholder.RoleSelectExpansion;
import com.a3v1k.flightSchool.domain.team.Team;
import com.a3v1k.flightSchool.application.team.TeamManager;
import com.a3v1k.flightSchool.platform.paper.display.TeamVisualManager;
import com.a3v1k.flightSchool.platform.paper.config.ConfigManager;
import com.destroystokyo.paper.Title;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@Getter
public class FlightSchool extends JavaPlugin {

    private static final double LOCATOR_BAR_RANGE = 60_000_000.0D;

    @Getter
    private static FlightSchool instance;

    private GameManager gameManager;
    private TeamManager teamManager;
    private TeamVisualManager teamVisualManager;
    private LobbyManager lobbyManager;
    private ConfigManager configManager;
    private KillcamManager killcamManager;
    private BukkitTask locatorBarTask;
    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("FlightSchool is enabling...");

        initializeManagers();
        startLocatorBarGuard();

        this.initializeTeams();
        this.getLogger().info("FlightSchool has been enabled.");

        this.enableCommands();
        this.enableEvents();

        new RoleSelectExpansion(this).register();
        new PointsExpansion(this).register();

        this.teamVisualManager.refreshAll();
    }

    private void initializeManagers() {
        if (!getServer().getPluginManager().isPluginEnabled("CreatorSplashCore")) {
            getLogger().severe("CreatorSplashCore must be enabled before FlightSchool can start.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        gameManager = new GameManager();
        lobbyManager = new LobbyManager();
        teamManager = new TeamManager();
        teamVisualManager = new TeamVisualManager();
        configManager = new ConfigManager();
        killcamManager = new KillcamManager();
    }

    private void initializeTeams() {
        this.gameManager.addTeam(new Team("a", "red", Color.RED, "red_spawn"));
        this.gameManager.addTeam(new Team("b", "yellow", Color.YELLOW, "yellow_spawn"));
        this.gameManager.addTeam(new Team("c", "green", Color.GREEN, "green_spawn"));
        this.gameManager.addTeam(new Team("d", "blue", Color.TEAL, "blue_spawn"));
        this.gameManager.addTeam(new Team("e", "dark_violet", Color.FUCHSIA, "darkviolet_spawn"));
        this.gameManager.addTeam(new Team("f", "violet", Color.PURPLE, "violet_spawn"));
        this.gameManager.addTeam(new Team("g", "dark_blue", Color.BLUE, "darkblue_spawn"));
        this.gameManager.addTeam(new Team("h", "orange", Color.ORANGE, "orange_spawn"));

        this.getLogger().info("Teams initialized.");
    }

    public void startGame() {
        this.getLogger().info("FlightSchool game is starting!");
        enableLocatorBar();
        this.lobbyManager.startCinematic(new ArrayList<>(this.getServer().getOnlinePlayers()));
    }

    public void stopGame() {
        this.getLogger().info("FlightSchool game has stopped.");
        enableLocatorBar();
        Title title = Title.builder()
                .fadeIn(5)
                .stay(40)
                .fadeOut(20)
                .title("Game Stopped")
                .subtitle("Returning to lobby")
                .build();

        this.gameManager.resetRoundState();

        for (Player player : Bukkit.getOnlinePlayers()) {
            resetPlayerToLobby(player, false);
            player.sendTitle(title);
        }
    }

    public void endGame(Map<UUID, Integer> scores, String winner) {
        this.getLogger().info("FlightSchool game has ended. Winner: " + winner);
    }

    @Override
    public void onDisable() {
        stopLocatorBarGuard();

        if (this.gameManager != null) {
            this.gameManager.resetRoundState();
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            resetPlayerToLobby(player, true);
        }

        this.getLogger().info("FlightSchool has been disabled.");
        instance = null;

    }

    public Location getLobbySpawnLocation() {
        if (this.configManager == null) {
            return Bukkit.getWorlds().getFirst().getSpawnLocation();
        }

        World world = getConfigManager().getCannonLocations().values().stream()
                .findFirst()
                .filter(locations -> locations != null && !locations.isEmpty())
                .map(locations -> locations.getFirst().getWorld())
                .orElse(Bukkit.getWorlds().getFirst());
        return world.getSpawnLocation();
    }

    public void resetPlayerToLobby(Player player, boolean persistLocation) {
        if (player == null || !player.isOnline()) {
            return;
        }

        enableLocatorBar();
        player.getInventory().clear();
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        player.teleport(getLobbySpawnLocation());
        player.setVelocity(new org.bukkit.util.Vector());
        player.setGameMode(GameMode.ADVENTURE);

        if (persistLocation) {
            player.saveData();
        }
    }

    public void enableCommands() {
        this.getCommand("fsh").setExecutor(new FlightSchoolCommand(this));
        this.getCommand("fsh-test").setExecutor(new TempCommands(this));
    }

    public void enableEvents() {
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        this.getServer().getPluginManager().registerEvents(new GameListener(this), this);
        this.getServer().getPluginManager().registerEvents(new TeamListener(this), this);
        this.getServer().getPluginManager().registerEvents(this.killcamManager, this);
    }

    private void startLocatorBarGuard() {
        stopLocatorBarGuard();
        this.locatorBarTask = Bukkit.getScheduler().runTaskTimer(this, this::enableLocatorBar, 0L, 1L);
    }

    private void stopLocatorBarGuard() {
        if (this.locatorBarTask == null) {
            return;
        }

        this.locatorBarTask.cancel();
        this.locatorBarTask = null;
    }

    public void enableLocatorBar() {
        enableLocatorBarOnAllWorlds();
        enableLocatorBarForOnlinePlayers();
    }

    public void enableLocatorBarForOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            enableLocatorBarForPlayer(player);
        }
    }

    public void enableLocatorBarOnAllWorlds() {
        for (World world : Bukkit.getWorlds()) {
            if (!Boolean.TRUE.equals(world.getGameRuleValue(GameRule.LOCATOR_BAR))) {
                world.setGameRule(GameRule.LOCATOR_BAR, true);
            }
        }
    }

    public void enableLocatorBarForPlayer(Player player) {
        restoreLocatorBarAttribute(player, Attribute.WAYPOINT_RECEIVE_RANGE);
        restoreLocatorBarAttribute(player, Attribute.WAYPOINT_TRANSMIT_RANGE);
    }

    private void restoreLocatorBarAttribute(Player player, Attribute attribute) {
        AttributeInstance attributeInstance = player.getAttribute(attribute);
        if (attributeInstance == null || attributeInstance.getBaseValue() >= LOCATOR_BAR_RANGE) {
            return;
        }

        attributeInstance.setBaseValue(LOCATOR_BAR_RANGE);
    }

    public ScoreManager getScoreManager() {
        return this.gameManager.getScoreManager();
    }

}
