package com.a3v1k.flightSchool.placeholders;

import com.a3v1k.flightSchool.FlightSchool;
import com.a3v1k.flightSchool.player.GamePlayer;
import com.a3v1k.flightSchool.player.Role;
import com.a3v1k.flightSchool.team.Team;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RoleSelectExpansion extends PlaceholderExpansion {

    private final FlightSchool plugin;

    public RoleSelectExpansion(FlightSchool plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "fsh";
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

        if (params.equalsIgnoreCase("cannons")) {
            return String.valueOf(this.plugin.getGameManager().getRoleCount(team, Role.CANNON_OPERATOR));
        }


        if (params.equalsIgnoreCase("planes")) {
            return String.valueOf(this.plugin.getGameManager().getRoleCount(team, Role.PLANE_PILOT));
        }

        return null;
    }
}
