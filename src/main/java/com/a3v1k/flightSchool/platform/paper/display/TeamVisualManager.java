package com.a3v1k.flightSchool.platform.paper.display;

import com.a3v1k.flightSchool.domain.team.Team;
import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import com.a3v1k.flightSchool.domain.player.GamePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Map;

public class TeamVisualManager {

    private final FlightSchool plugin;
    private static final Map<String, String> ITEMSADDER_TEAM_IMAGES = Map.of(
            "red", "orca",
            "yellow", "seahorse",
            "green", "turtle",
            "blue", "dolphin",
            "dark_violet", "stingray",
            "violet", "jellyfish",
            "dark_blue", "swordfish",
            "orange", "octopus"
    );

    public TeamVisualManager() {
        this.plugin = FlightSchool.getInstance();
    }

    public void syncPlayer(Player player) {
        if (player == null) {
            return;
        }

        applyPlayerListName(player);
        syncAllViewerScoreboards();
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyPlayerListName(player);
        }

        syncAllViewerScoreboards();
    }

    private void syncAllViewerScoreboards() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            syncViewerScoreboard(viewer.getScoreboard());
        }
    }

    private void syncViewerScoreboard(Scoreboard scoreboard) {
        if (scoreboard == null) {
            return;
        }

        for (Team configuredTeam : plugin.getGameManager().getTeams().values()) {
            org.bukkit.scoreboard.Team scoreboardTeam = scoreboard.getTeam(getScoreboardTeamName(configuredTeam));
            if (scoreboardTeam == null) {
                scoreboardTeam = scoreboard.registerNewTeam(getScoreboardTeamName(configuredTeam));
            }

            configureScoreboardTeam(scoreboardTeam, configuredTeam);
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            GamePlayer gamePlayer = plugin.getGameManager().getGamePlayer(onlinePlayer);
            Team assignedTeam = gamePlayer == null ? null : gamePlayer.getTeam();
            String entry = onlinePlayer.getName();

            for (Team configuredTeam : plugin.getGameManager().getTeams().values()) {
                org.bukkit.scoreboard.Team scoreboardTeam = scoreboard.getTeam(getScoreboardTeamName(configuredTeam));
                if (scoreboardTeam == null) {
                    continue;
                }

                if (configuredTeam == assignedTeam) {
                    if (!scoreboardTeam.hasEntry(entry)) {
                        scoreboardTeam.addEntry(entry);
                    }
                } else if (scoreboardTeam.hasEntry(entry)) {
                    scoreboardTeam.removeEntry(entry);
                }
            }
        }
    }

    private void applyPlayerListName(Player player) {
        GamePlayer gamePlayer = plugin.getGameManager().getGamePlayer(player);
        Team team = gamePlayer == null ? null : gamePlayer.getTeam();

        if (isTabPluginEnabled()) {
            if (team == null) {
                resetTabPluginStyle(player);
            } else {
                applyTabPluginStyle(player, team);
            }
            return;
        }

        if (team == null) {
            player.playerListName(Component.text(player.getName()));
            return;
        }

        TextColor color = getTabColor(team);
        player.playerListName(Component.text(player.getName(), color));
    }

    public void applyTabPluginStyle(Player player, Team team) {
        if (player == null || team == null || !isTabPluginEnabled()) {
            return;
        }

        String imageName = ITEMSADDER_TEAM_IMAGES.getOrDefault(team.getName(), "");
        String customName = "\"" + getTabColorCode(team) + player.getName() + "\"";

        if (!imageName.isEmpty()) {
            String tabPrefix = "\"%img_" + imageName + "% \"";
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "tab player " + player.getName() + " tabprefix " + tabPrefix);
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "tab player " + player.getName() + " tabprefix");
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "tab player " + player.getName() + " customtabname " + customName);
    }

    public void resetTabPluginStyle(Player player) {
        if (player == null || !isTabPluginEnabled()) {
            return;
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "tab player " + player.getName() + " tabprefix");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "tab player " + player.getName() + " customtabname");
    }

    private void configureScoreboardTeam(org.bukkit.scoreboard.Team scoreboardTeam, Team configuredTeam) {
        scoreboardTeam.color(getNametagColor(configuredTeam));
        scoreboardTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY, org.bukkit.scoreboard.Team.OptionStatus.ALWAYS);
    }

    private String getScoreboardTeamName(Team team) {
        return team.getName();
    }

    private TextColor getTabColor(Team team) {
        return switch (team.getName()) {
            case "red" -> NamedTextColor.RED;
            case "yellow" -> NamedTextColor.YELLOW;
            case "green" -> NamedTextColor.GREEN;
            case "blue" -> NamedTextColor.AQUA;
            case "dark_violet" -> NamedTextColor.DARK_PURPLE;
            case "violet" -> TextColor.color(0xF58EEB);
            case "dark_blue" -> NamedTextColor.BLUE;
            case "orange" -> NamedTextColor.GOLD;
            default -> NamedTextColor.WHITE;
        };
    }

    private NamedTextColor getNametagColor(Team team) {
        return switch (team.getName()) {
            case "red" -> NamedTextColor.RED;
            case "yellow" -> NamedTextColor.YELLOW;
            case "green" -> NamedTextColor.GREEN;
            case "blue" -> NamedTextColor.AQUA;
            case "dark_violet" -> NamedTextColor.DARK_PURPLE;
            case "violet" -> NamedTextColor.LIGHT_PURPLE;
            case "dark_blue" -> NamedTextColor.BLUE;
            case "orange" -> NamedTextColor.GOLD;
            default -> NamedTextColor.WHITE;
        };
    }

    private String getTabColorCode(Team team) {
        return switch (team.getName()) {
            case "red" -> "&c";
            case "yellow" -> "&e";
            case "green" -> "&a";
            case "blue" -> "&b";
            case "dark_violet" -> "&5";
            case "violet" -> "&d";
            case "dark_blue" -> "&9";
            case "orange" -> "&6";
            default -> "&f";
        };
    }

    private boolean isTabPluginEnabled() {
        return plugin.getServer().getPluginManager().isPluginEnabled("TAB");
    }
}
