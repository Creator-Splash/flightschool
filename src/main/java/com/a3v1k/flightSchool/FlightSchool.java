package com.a3v1k.flightSchool;

import com.a3v1k.flightSchool.commands.FlightSchoolCommand;
import com.a3v1k.flightSchool.commands.TempCommands;
import com.a3v1k.flightSchool.game.GameManager;
import com.a3v1k.flightSchool.game.LobbyManager;
import com.a3v1k.flightSchool.killcam.KillcamManager;
import com.a3v1k.flightSchool.listeners.GameListener;
import com.a3v1k.flightSchool.listeners.PlayerListener;
import com.a3v1k.flightSchool.listeners.TeamListener;
import com.a3v1k.flightSchool.placeholders.PointsExpansion;
import com.a3v1k.flightSchool.placeholders.RoleSelectExpansion;
import com.a3v1k.flightSchool.team.Team;
import com.a3v1k.flightSchool.team.TeamManager;
import com.a3v1k.flightSchool.util.ConfigManager;
import creatorsplash.creatorsplashcore.api.ProxyConnector;
import org.bukkit.Color;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public final class FlightSchool extends JavaPlugin {

    private static FlightSchool instance;

    private GameManager gameManager;
    private TeamManager teamManager;
    private LobbyManager lobbyManager;
    private ConfigManager configManager;
    private com.a3v1k.flightSchool.killcam.KillcamManager killcamManager;
    private ProxyConnector proxyConnector;

    @Override
    public void onEnable() {
        instance = this; // Set the static instance
        this.getLogger().info("FlightSchool is enabling...");
        this.proxyConnector = ProxyConnector.getInstance();
        this.gameManager = new GameManager(this);
        this.lobbyManager = new LobbyManager(this);
        this.teamManager = new TeamManager(this);
        this.configManager = new ConfigManager(this);
        this.killcamManager = new KillcamManager(this);

        this.initializeTeams();
        this.getLogger().info("FlightSchool has been enabled.");

        this.enableCommands();
        this.enableEvents();

        new RoleSelectExpansion(this).register();
        new PointsExpansion(this).register();
    }

    private void initializeTeams() {
        this.gameManager.addTeam(new Team("red", Color.RED, "red_spawn"));
        this.gameManager.addTeam(new Team("yellow", Color.YELLOW, "yellow_spawn"));
        this.gameManager.addTeam(new Team("green", Color.GREEN, "green_spawn"));
        this.gameManager.addTeam(new Team("blue", Color.TEAL, "blue_spawn"));
        this.gameManager.addTeam(new Team("dark_violet", Color.FUCHSIA, "darkviolet_spawn"));
        this.gameManager.addTeam(new Team("violet", Color.PURPLE, "violet_spawn"));
        this.gameManager.addTeam(new Team("dark_blue", Color.BLUE, "darkblue_spawn"));
        this.gameManager.addTeam(new Team("orange", Color.ORANGE, "orange_spawn"));
        this.getLogger().info("Teams initialized.");
    }

    public void startGame() {
        this.getLogger().info("FlightSchool game is starting!");
        this.lobbyManager.startCinematic(new ArrayList<>(this.getServer().getOnlinePlayers()));
    }

    public void endGame(Map<UUID, Integer> scores, String winner) {
        this.getLogger().info("FlightSchool game has ended. Winner: " + winner);
    }

    @Override
    public void onDisable() {
        this.getLogger().info("FlightSchool has been disabled.");
        instance = null;
    }

    public static FlightSchool getInstance() {
        return instance;
    }

    public GameManager getGameManager() {
        return this.gameManager;
    }
    public TeamManager getTeamManager() {
        return this.teamManager;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public ProxyConnector getProxyConnector() {
        return this.proxyConnector;
    }

    public KillcamManager getKillcamManager() {
        return this.killcamManager;
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
//        this.getServer().getPluginManager().registerEvents(new MythicListener(this), this);
    }

}
