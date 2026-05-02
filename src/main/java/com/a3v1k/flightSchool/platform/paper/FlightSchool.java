package com.a3v1k.flightSchool.platform.paper;

import com.a3v1k.flightSchool.application.scheduler.Scheduler;
import com.a3v1k.flightSchool.platform.paper.command.CommandRegistrar;
import com.a3v1k.flightSchool.platform.paper.command.FsAdminCommands;
import com.a3v1k.flightSchool.platform.paper.command.FsCommands;
import com.a3v1k.flightSchool.application.game.*;
import com.a3v1k.flightSchool.platform.paper.game.PaperGameManager;
import com.a3v1k.flightSchool.platform.paper.game.PaperLobbyManager;
import com.a3v1k.flightSchool.platform.paper.integration.FlightSchoolGameAdapter;
import com.a3v1k.flightSchool.platform.paper.integration.scheduler.PaperSchedulerAdapter;
import com.a3v1k.flightSchool.platform.paper.listener.GameListener;
import com.a3v1k.flightSchool.platform.paper.listener.PlayerListener;
import com.a3v1k.flightSchool.platform.paper.listener.TeamListener;
import com.a3v1k.flightSchool.platform.paper.display.placeholder.PointsExpansion;
import com.a3v1k.flightSchool.platform.paper.display.placeholder.RoleSelectExpansion;
import com.a3v1k.flightSchool.domain.team.Team;
import com.a3v1k.flightSchool.application.team.TeamManager;
import com.a3v1k.flightSchool.platform.paper.display.TeamVisualManager;
import com.a3v1k.flightSchool.platform.paper.config.ConfigManager;
import creatorsplash.creatorsplashcore.api.CreatorSplashCore;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Getter
public class FlightSchool extends JavaPlugin {

    private static final double LOCATOR_BAR_RANGE = 60_000_000.0D;

    @Getter private static FlightSchool instance;

    @Getter private Scheduler scheduler;
    @Getter private GameManager gameManager;
    @Getter private GameOrchestrator gameOrchestrator;

    @Getter private CommandRegistrar commandRegistrar;

    private TeamManager teamManager;
    private TeamVisualManager teamVisualManager;
    private PaperLobbyManager lobbyManager;
    private ConfigManager configManager;
    private KillcamManager killcamManager;
    private Scheduler.Task locatorBarTask;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("FlightSchool is enabling...");

        this.scheduler = new PaperSchedulerAdapter(this);

        initializeManagers();
        startLocatorBarGuard();
        initializeTeams();

        getLogger().info("FlightSchool has been enabled.");

        enableCommands();
        enableEvents();

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

        CreatorSplashCore.register(this, new FlightSchoolGameAdapter(this));

        PaperGameManager paperGameManager = new PaperGameManager(this, scheduler);
        this.gameManager = paperGameManager;
        this.gameOrchestrator = paperGameManager;

        this.lobbyManager = new PaperLobbyManager();
        this.teamManager = new TeamManager();
        this.teamVisualManager = new TeamVisualManager();
        this.configManager = new ConfigManager();
        this.killcamManager = new KillcamManager();
    }

    // TODO
    private void initializeTeams() {
        this.gameManager.addTeam(new Team("a", "red", Color.RED, "red_spawn"));
        this.gameManager.addTeam(new Team("b", "yellow", Color.YELLOW, "yellow_spawn"));
        this.gameManager.addTeam(new Team("c", "green", Color.GREEN, "green_spawn"));
        this.gameManager.addTeam(new Team("d", "blue", Color.TEAL, "blue_spawn"));
        this.gameManager.addTeam(new Team("e", "dark_violet", Color.FUCHSIA, "darkviolet_spawn"));
        this.gameManager.addTeam(new Team("f", "violet", Color.PURPLE, "violet_spawn"));
        this.gameManager.addTeam(new Team("g", "dark_blue", Color.BLUE, "darkblue_spawn"));
        this.gameManager.addTeam(new Team("h", "orange", Color.ORANGE, "orange_spawn"));

        getLogger().info("Teams initialized.");
    }

    public void startGame() {
        getLogger().info("FlightSchool game is starting!");
        enableLocatorBar();
        this.lobbyManager.startCinematic(
            this.getServer().getOnlinePlayers().stream()
                .map(Player::getUniqueId)
                .toList()
        );
    }

    public void stopGame() {
        getLogger().info("FlightSchool game has stopped.");
        enableLocatorBar();

        this.gameOrchestrator.resetRoundState();

        Component title = Component.text("Game Stopped", NamedTextColor.RED);
        Component subtitle = Component.text("Returning to lobby", NamedTextColor.GRAY);
        Title adventureTitle = Title.title(
            title,
            subtitle,
            Title.Times.times(
                Duration.ofMillis(250),
                Duration.ofSeconds(2),
                Duration.ofMillis(1000)
            )
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            resetPlayerToLobby(player, false);
            player.showTitle(adventureTitle);
        }
    }

    public void endGame(Map<UUID, Integer> scores, String winner) {
        getLogger().info("FlightSchool game has ended. Winner: " + winner);
    }

    @Override
    public void onDisable() {
        stopLocatorBarGuard();

        if (this.gameOrchestrator != null) {
            this.gameOrchestrator.resetRoundState();
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            resetPlayerToLobby(player, true);
        }

        if (this.scheduler != null) {
            this.scheduler.onDisable();
        }

        getLogger().info("FlightSchool has been disabled.");
        instance = null;
    }

    public Location getLobbySpawnLocation() {
        if (this.configManager == null) {
            return Bukkit.getWorlds().getFirst().getSpawnLocation();
        }

        World world = getConfigManager().getCannonLocations().values().stream()
            .findFirst()
            .filter(locations -> !locations.isEmpty())
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
        try {
            this.commandRegistrar = new CommandRegistrar(this);
            this.commandRegistrar.registerAnnotated(new FsCommands(this));
            this.commandRegistrar.registerAnnotated(new FsAdminCommands(this));
        } catch (Exception e) {
            getLogger().severe("Failed to initialize command registrar: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    public void enableEvents() {
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        this.getServer().getPluginManager().registerEvents(new GameListener(this), this);
        this.getServer().getPluginManager().registerEvents(new TeamListener(this), this);
        this.getServer().getPluginManager().registerEvents(this.killcamManager, this);
    }

    private void startLocatorBarGuard() {
        stopLocatorBarGuard();
        this.locatorBarTask = scheduler.runRepeating(this::enableLocatorBar, 0L, 1L);
    }

    private void stopLocatorBarGuard() {
        if (this.locatorBarTask == null) return;
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
