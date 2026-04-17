package com.a3v1k.flightSchool;

import com.a3v1k.flightSchool.commands.FlightSchoolCommand;
import com.a3v1k.flightSchool.commands.TempCommands;
import com.a3v1k.flightSchool.game.*;
import com.a3v1k.flightSchool.listeners.GameListener;
import com.a3v1k.flightSchool.listeners.PlayerListener;
import com.a3v1k.flightSchool.listeners.TeamListener;
import com.a3v1k.flightSchool.placeholders.PointsExpansion;
import com.a3v1k.flightSchool.placeholders.RoleSelectExpansion;
import com.a3v1k.flightSchool.player.GamePlayer;
import com.a3v1k.flightSchool.team.Team;
import com.a3v1k.flightSchool.team.TeamManager;
import com.a3v1k.flightSchool.util.ConfigManager;
import com.destroystokyo.paper.Title;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@Getter
public final class FlightSchool extends JavaPlugin {

    @Getter
    private static FlightSchool instance;

    private GameManager gameManager;
    private TeamManager teamManager;
    private LobbyManager lobbyManager;
    private ConfigManager configManager;
    private KillcamManager killcamManager;
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
        if (!getServer().getPluginManager().isPluginEnabled("CreatorSplashCore")) {
            getLogger().severe("CreatorSplashCore must be enabled before FlightSchool can start.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        gameManager = new GameManager();
        lobbyManager = new LobbyManager();
        teamManager = new TeamManager();
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
        this.lobbyManager.startCinematic(new ArrayList<>(this.getServer().getOnlinePlayers()));
    }

    public void stopGame() {
        this.getLogger().info("FlightSchool game has stopped.");
        World world = getConfigManager().getCannonLocations().values().stream()
                .findFirst()
                .map(locations -> locations.getFirst().getWorld())
                .orElse(Bukkit.getWorlds().getFirst());
        Location lobbyLocation = world.getSpawnLocation();
        Title title = Title.builder()
                .fadeIn(5)
                .stay(40)
                .fadeOut(20)
                .title("Game Stopped")
                .subtitle("Returning to lobby")
                .build();

        this.gameManager.resetRoundState();

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().clear();
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
            player.teleport(lobbyLocation);
            player.setGameMode(GameMode.ADVENTURE);
            player.sendTitle(title);
        }
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

    public ScoreManager getScoreManager() {
        return this.gameManager.getScoreManager();
    }

}
