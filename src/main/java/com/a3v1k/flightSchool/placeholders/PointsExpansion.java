package com.a3v1k.flightSchool.placeholders;

import com.a3v1k.flightSchool.FlightSchool;
import com.a3v1k.flightSchool.player.GamePlayer;
import com.a3v1k.flightSchool.player.Role;
import com.a3v1k.flightSchool.team.Team;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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

        if (params.equalsIgnoreCase("timer")) {
            if(this.plugin.getGameManager().getMainTimer() == null) {
                return String.valueOf("NOT STARTED");
            }

            return String.valueOf(this.plugin.getGameManager().getMainTimer().getTime());
        }


        if (params.equalsIgnoreCase("health_blimp")) {
            if(this.plugin.getGameManager().getHealthManager() == null) {
                return "NOT STARTED";
            }

            if(this.plugin.getGameManager().getHealthManager().get(team.getName()) == null) {
                return "NOT STARTED";
            }

            return ChatColor.BLUE + String.valueOf(this.plugin.getGameManager().getHealthManager().get(team.getName()).getHealth()) + "%";
        }

        if (params.equalsIgnoreCase("health_1")) {
            if(this.plugin.getGameManager().getTeamPlanes() == null) {
                return "NOT STARTED";
            }

            if(this.plugin.getGameManager().getTeamPlanes().get(team).isEmpty()) {
                return "NOT STARTED";
            }

            if(this.plugin.getGameManager().getTeamPlanes().get(team).get(0) == null) {
                return ChatColor.GOLD + "⚠ PLANE INACTIVE ⚠";
            }

            return ChatColor.GOLD + String.valueOf(this.plugin.getGameManager().getTeamPlanes().get(team).get(0).getEntity().getHealth() / this.plugin.getGameManager().getTeamPlanes().get(team).get(0).getEntity().getMaxHealth() * 100) + "%";
        }

        if (params.equalsIgnoreCase("health_2")) {
            if(this.plugin.getGameManager().getTeamPlanes() == null) {
                return "NOT STARTED";
            }

            if(this.plugin.getGameManager().getTeamPlanes().get(team).isEmpty()) {
                return "NOT STARTED";
            }

            if(this.plugin.getGameManager().getTeamPlanes().get(team).get(1) == null) {
                return ChatColor.GOLD + "⚠ PLANE INACTIVE ⚠";
            }

            return ChatColor.GOLD + String.valueOf(this.plugin.getGameManager().getTeamPlanes().get(team).get(1).getEntity().getHealth() / this.plugin.getGameManager().getTeamPlanes().get(team).get(1).getEntity().getMaxHealth() * 100) + "%";
        }

        if (params.equalsIgnoreCase("health_3")) {
            if(this.plugin.getGameManager().getTeamPlanes() == null) {
                return "NOT STARTED";
            }

            if(this.plugin.getGameManager().getTeamPlanes().get(team).isEmpty()) {
                return "NOT STARTED";
            }

            if(this.plugin.getGameManager().getTeamPlanes().get(team).get(2) == null) {
                return ChatColor.GOLD + "⚠ PLANE INACTIVE ⚠";
            }

            return ChatColor.GOLD + String.valueOf(this.plugin.getGameManager().getTeamPlanes().get(team).get(2).getEntity().getHealth() / this.plugin.getGameManager().getTeamPlanes().get(team).get(2).getEntity().getMaxHealth() * 100) + "%";
        }

        if (params.equalsIgnoreCase("scores_value_1")) {
            return String.valueOf(0);
        }
        if (params.equalsIgnoreCase("scores_value_2")) {
            return String.valueOf(0);
        }
        if (params.equalsIgnoreCase("scores_value_3")) {
            return String.valueOf(0);
        }
        if (params.equalsIgnoreCase("scores_value_4")) {
            return String.valueOf(0);
        }
        if (params.equalsIgnoreCase("scores_value_5")) {
            return String.valueOf(0);
        }
        if (params.equalsIgnoreCase("scores_value_6")) {
            return String.valueOf(0);
        }
        if (params.equalsIgnoreCase("scores_value_7")) {
            return String.valueOf(0);
        }
        if (params.equalsIgnoreCase("scores_value_8")) {
            return String.valueOf(0);
        }

        if (params.equalsIgnoreCase("scores_name_1")) {
            return "Red";
        }
        if (params.equalsIgnoreCase("scores_name_2")) {
            return "Yellow";
        }
        if (params.equalsIgnoreCase("scores_name_3")) {
            return "Green";
        }
        if (params.equalsIgnoreCase("scores_name_4")) {
            return "Blue";
        }
        if (params.equalsIgnoreCase("scores_name_5")) {
            return "Dark Violet";
        }
        if (params.equalsIgnoreCase("scores_name_6")) {
            return "Violet";
        }
        if (params.equalsIgnoreCase("scores_name_7")) {
            return "Dark Blue";
        }
        if (params.equalsIgnoreCase("scores_name_8")) {
            return "Orange";
        }


        return null;
    }
}
