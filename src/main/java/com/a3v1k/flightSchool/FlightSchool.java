package com.a3v1k.flightSchool;

import com.a3v1k.flightSchool.commands.FlightSchoolCommand;
import com.a3v1k.flightSchool.commands.TempCommands;
import com.a3v1k.flightSchool.game.GameManager;
import com.a3v1k.flightSchool.game.LobbyManager;
import com.a3v1k.flightSchool.game.KillcamManager;
import com.a3v1k.flightSchool.game.ScoreManager;
import com.a3v1k.flightSchool.listeners.GameListener;
import com.a3v1k.flightSchool.listeners.PlayerListener;
import com.a3v1k.flightSchool.listeners.TeamListener;
import com.a3v1k.flightSchool.placeholders.PointsExpansion;
import com.a3v1k.flightSchool.placeholders.RoleSelectExpansion;
import com.a3v1k.flightSchool.team.Team;
import com.a3v1k.flightSchool.team.TeamManager;
import com.a3v1k.flightSchool.util.ConfigManager;
import creatorsplash.creatorsplashcore.api.ProxyConnector;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@Getter
public final class FlightSchool extends JavaPlugin {

    @Getter
    private static FlightSchool instance;

    private ScoreManager scoreManager;
    private GameManager gameManager;
    private TeamManager teamManager;
    private LobbyManager lobbyManager;
    private ConfigManager configManager;
    private KillcamManager killcamManager;
    private ProxyConnector proxyConnector;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("FlightSchool is enabling...");

        initializeManagers();

        this.initializeTeams();
        this.getLogger().info("FlightSchool has been enabled.");

        this.enableCommands();
        this.enableEvents();

        new RoleSelectExpansion(this).register();
        new PointsExpansion(this).register();
    }

    private void initializeManagers() {
        proxyConnector = ProxyConnector.getInstance();
        gameManager = new GameManager();
        lobbyManager = new LobbyManager();
        teamManager = new TeamManager();
        configManager = new ConfigManager();
        killcamManager = new KillcamManager();
        scoreManager = new ScoreManager();
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

}
