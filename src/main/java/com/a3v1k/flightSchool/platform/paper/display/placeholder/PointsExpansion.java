package com.a3v1k.flightSchool.platform.paper.display.placeholder;

import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import com.a3v1k.flightSchool.application.game.ScoreManager;
import com.a3v1k.flightSchool.domain.player.GamePlayer;
import com.a3v1k.flightSchool.domain.player.Role;
import com.a3v1k.flightSchool.domain.team.Team;
import dev.lone.itemsadder.api.FontImages.FontImageWrapper;
import dev.lone.itemsadder.api.ItemsAdder;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
        if(params.equalsIgnoreCase("timer")){
            if(this.plugin.getGameManager().getGameStartedAt() == -1) {
                return "Starting soon!";
            }

            long time = this.plugin.getGameManager().getGameStartedAt();
            if(System.currentTimeMillis() - time > 1000 * 20) time -= 1000 * 20;

            return "Time: " + new SimpleDateFormat("mm:ss").format(new Date(System.currentTimeMillis() - time));
        }

        if(params.startsWith("scores_name")){
            return switch (params.toLowerCase()){
                case "scores_name_1" -> "\uDAC0\uDC04 <red>Orca</red>";

                case "scores_name_2" -> "\uDAC0\uDC05 <yellow>Seahorse</yellow>";

                case "scores_name_3" -> "\uDAC0\uDC08 <green>Turtle</green>";

                case "scores_name_4" -> "\uDAC0\uDC01 <aqua>Dolphin</aqua>";

                case "scores_name_5" -> "\uDAC0\uDC06 <dark_purple>Stingray</dark_purple>";

                case "scores_name_6" -> "\uDAC0\uDC02 <#f58eeb>Jellyfish</#f58eeb>";

                case "scores_name_7" -> "\uDAC0\uDC07 <blue>Swordfish</blue>";

                case "scores_name_8" -> "\uDAC0\uDC03 <gold>Octopus</gold>";
                default -> "";
            };
        }
        if (player == null) return "";

        GamePlayer gamePlayer = this.plugin.getGameManager().getGamePlayer(player);
        if (gamePlayer == null) return "";

        Team team = gamePlayer.getTeam();
        if (team == null) return "";

        return switch (params.toLowerCase()){
            case "health_blimp" -> {
                if(this.plugin.getGameManager().getHealthManager() == null) {
                    yield  "";
                }

                if(this.plugin.getGameManager().getHealthManager().get(team.getName()) == null) {
                    yield "";
                }

                yield " | Blimp Health: " + this.plugin.getGameManager().getHealthManager().get(team.getName()).getHealth() + "%";
            }

            case "boost_hint" -> getBoostHint(gamePlayer);

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

            default -> null;
        };
    }

    private String getBoostHint(GamePlayer gamePlayer) {
        if (!Role.PLANE_PILOT.equals(gamePlayer.getRole())) {
            return "";
        }

        return " | <img:right_click> to boost";
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
