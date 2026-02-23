package com.a3v1k.flightSchool.placeholders;

import com.a3v1k.flightSchool.FlightSchool;
import com.a3v1k.flightSchool.game.ScoreManager;
import com.a3v1k.flightSchool.player.GamePlayer;
import com.a3v1k.flightSchool.player.Role;
import com.a3v1k.flightSchool.team.Team;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PointsExpansion extends PlaceholderExpansion {

    private final FlightSchool plugin;

    public PointsExpansion(FlightSchool plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "flightschool";
    }

    @Override
    public @NotNull String getAuthor() {
        return "a3v1k";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        GamePlayer gamePlayer = this.plugin.getGameManager().getGamePlayer(player);
        if (gamePlayer == null) return "";

        Team team = gamePlayer.getTeam();
        if (team == null) return "";

        return switch (params.toLowerCase()){
            case "timer" -> {
                if(this.plugin.getGameManager().getGameStartedAt() == -1) {
                    yield "Starting soon!";
                }

                yield "Time: " + new SimpleDateFormat("mm:ss").format(new Date(System.currentTimeMillis() - this.plugin.getGameManager().getGameStartedAt()));
            }

            case "health_blimp" -> {
                if(this.plugin.getGameManager().getHealthManager() == null) {
                    yield  "";
                }

                if(this.plugin.getGameManager().getHealthManager().get(team.getName()) == null) {
                    yield "";
                }

                yield " | Blimp Health: " + this.plugin.getGameManager().getHealthManager().get(team.getName()).getHealth() + "%";
            }

            case "score" -> String.valueOf(plugin.getScoreManager().getScore(team));

            case "health_1" -> getPlaneHealth(team, 0);
            case "health_2" -> getPlaneHealth(team, 1);
            case "health_3" ->getPlaneHealth(team, 2);

            case "scores_value_1" -> String.valueOf(plugin.getScoreManager().getScore(plugin.getGameManager().getTeam("red")));

            case "scores_value_2" -> String.valueOf(plugin.getScoreManager().getScore(plugin.getGameManager().getTeam("yellow")));

            case "scores_value_3" -> String.valueOf(plugin.getScoreManager().getScore(plugin.getGameManager().getTeam("green")));

            case "scores_value_4" -> String.valueOf(plugin.getScoreManager().getScore(plugin.getGameManager().getTeam("blue")));

            case "scores_value_5" -> String.valueOf(plugin.getScoreManager().getScore(plugin.getGameManager().getTeam("dark_violet")));

            case "scores_value_6" -> String.valueOf(plugin.getScoreManager().getScore(plugin.getGameManager().getTeam("violet")));

            case "scores_value_7" -> String.valueOf(plugin.getScoreManager().getScore(plugin.getGameManager().getTeam("dark_blue")));

            case "scores_value_8" -> String.valueOf(plugin.getScoreManager().getScore(plugin.getGameManager().getTeam("orange")));

            case "scores_name_1" -> "Red";

            case "scores_name_2" -> "Yellow";

            case "scores_name_3" -> "Green";

            case "scores_name_4" -> "Blue";

            case "scores_name_5" -> "Dark Violet";

            case "scores_name_6" -> "Violet";

            case "scores_name_7" -> "Dark Blue";

            case "scores_name_8" -> "Orange";

            default -> null;
        };
    }

    private String getPlaneHealth(Team team, int planeIndex){
        if(this.plugin.getGameManager().getTeamPlanes() == null) {
            return "";
        }

        if(this.plugin.getGameManager().getTeamPlanes().get(team).isEmpty()) {
            return "";
        }

        if(this.plugin.getGameManager().getTeamPlanes().get(team).get(planeIndex) == null) {
            return "⚠ PLANE INACTIVE ⚠";
        }

        double planeHealth = this.plugin.getGameManager().getTeamPlanes().get(team).get(planeIndex).getEntity().getHealth();
        double maxHealth = this.plugin.getGameManager().getTeamPlanes().get(team).get(planeIndex).getEntity().getMaxHealth();

        return String.valueOf(planeHealth / maxHealth * 100) + "%";
    }
}
