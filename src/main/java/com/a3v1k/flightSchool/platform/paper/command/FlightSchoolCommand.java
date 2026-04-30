package com.a3v1k.flightSchool.platform.paper.command;

import com.a3v1k.flightSchool.application.blimp.FlightRunnable;
import com.a3v1k.flightSchool.application.game.GameManager;
import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import com.a3v1k.flightSchool.domain.match.GameState;
import com.a3v1k.flightSchool.domain.player.GamePlayer;
import com.a3v1k.flightSchool.domain.player.Role;
import com.a3v1k.flightSchool.domain.team.Team;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.mount.controller.MountControllerTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Level;

public class FlightSchoolCommand implements CommandExecutor {

    private final FlightSchool plugin;

    public FlightSchoolCommand(FlightSchool plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        switch (args[0].toLowerCase()){
            case "start" -> {
                if (plugin.getGameManager().getGameState() != GameState.LOBBY) {
                    sender.sendMessage("The game has already started.");
                    return true;
                }
                plugin.startGame();
                sender.sendMessage("The Flight School game has started! Role selection is now active.");
            }

            case "stop" -> {
                if(plugin.getGameManager().getGameState() == GameState.LOBBY){
                    sender.sendMessage("The game is already in the lobby state.");
                    return true;
                }

                plugin.stopGame();
                sender.sendMessage("The Flight School game has stopped.");
            }

            case "set-cannon" -> {
                if(!(sender instanceof Player player)) return true;

                Location location = player.getLocation();
                this.plugin.getConfigManager().addCannonLocation(args[1], location);
                player.sendMessage("set successfully - cannon for team " + args[1]);
            }

            case "set-plane" -> {
                if(!(sender instanceof Player player)) return true;

                Location location = player.getLocation();
                this.plugin.getConfigManager().addPlaneLocation(args[1], location);
                player.sendMessage("set successfully - plane for team " + args[1]);
            }

            case "team" -> {
                if(!(sender instanceof Player player)) return true;

                if(args.length != 3) {
                    player.sendMessage("Usage: /fsh team <player name> <team name>");
                    return true;
                }

                Team team = plugin.getGameManager().getTeam(args[2]);
                if(team == null) {
                    player.sendMessage("Team " + args[2] + " does not exist.");
                    return true;
                }

                Player target = plugin.getServer().getPlayer(args[1]);
                if(target == null) {
                    player.sendMessage("Player " + args[1] + " does not exist.");
                    return true;
                }

                GamePlayer gamePlayer = plugin.getGameManager().getGamePlayer(target);
                if(gamePlayer == null) {
                    player.sendMessage("Player " + args[1] + " does not exist.");
                    return true;
                }

                plugin.getGameManager().assignPlayerToTeam(target, team);
                player.sendMessage("You've set " + args[1] +  " in the " + team.getName() + " team.");
            }

            case "playing-as" -> {
                if(!(sender instanceof Player player)) return true;

                if (plugin.getGameManager().getGameState() != GameState.ROLE_SELECTION) {
                    player.sendMessage("You can only select a role during the role selection phase.");
                    return true;
                }
                GamePlayer gamePlayer = plugin.getGameManager().getGamePlayer(player);
                if (gamePlayer.getRole() != null) {
                    player.sendMessage("You have already selected a role.");
                    return true;
                }

                if (args.length == 1) return true;


                Role roleToAssign = args[1].equalsIgnoreCase("flight") ? Role.PLANE_PILOT : Role.CANNON_OPERATOR;

                if (!plugin.getGameManager().canAssignRole(gamePlayer.getTeam(), roleToAssign)) {
                    player.sendMessage("The " + roleToAssign.toString().replace('_', ' ') + " role is full for your team.");
                    return true;
                }

                plugin.getGameManager().assignRole(player, roleToAssign);
                player.sendMessage("You have been assigned as a " + roleToAssign.toString().replace('_', ' ') + ".");
                plugin.getLogger().log(Level.INFO, "{0} has been assigned as a {1}", new Object[]{player.getName(), roleToAssign});
            }

            case "planeprojectileblockhit" -> {
                // Expected:
                // /fsh planeprojectileblockhit <shooter_uuid> <shooter_name> <block_type> <world> <x> <y> <z>
                if (args.length < 8) {
                    plugin.getLogger().warning("Invalid plane projectile block hit command. Not enough arguments.");
                    return true;
                }

                UUID shooterUuid;
                try {
                    shooterUuid = UUID.fromString(args[1]);
                } catch (IllegalArgumentException exception) {
                    plugin.getLogger().warning("Invalid shooter UUID for plane projectile block hit: " + args[1]);
                    return true;
                }

                Player shooter = Bukkit.getPlayer(shooterUuid);
                if (shooter == null) {
                    plugin.getLogger().warning("Plane projectile shooter is not online: " + shooterUuid);
                    return true;
                }

                Material hitBlockType = Material.matchMaterial(args[3]);
                if (hitBlockType == null) {
                    plugin.getLogger().warning("Invalid hit block type for plane projectile block hit: " + args[3]);
                    return true;
                }


                World world = Bukkit.getWorld(args[4]);
                if (world == null) {
                    plugin.getLogger().warning("Invalid world for plane projectile block hit: " + args[4]);
                    return true;
                }

                double x;
                double y;
                double z;

                try {
                    x = Double.parseDouble(args[5]);
                    y = Double.parseDouble(args[6]);
                    z = Double.parseDouble(args[7]);
                } catch (NumberFormatException exception) {
                    plugin.getLogger().warning("Invalid location for plane projectile block hit.");
                    return true;
                }

                Location hitLocation = new Location(world, x, y, z);

                GameManager gameManager = FlightSchool.getInstance().getGameManager();

                GamePlayer gamePlayer = gameManager.getGamePlayer(shooter);
                Team playerTeam = gamePlayer == null ? null : gamePlayer.getTeam();
                Team nearestBlimpTeam = gameManager.getTeam(FlightRunnable.findNearestBlimpTeam(hitLocation));

                if (nearestBlimpTeam.getMembers().isEmpty()) return false;

                nearestBlimpTeam.getHealthManager().damage(1);
                if (playerTeam != nearestBlimpTeam) {
                }

            }

            default -> {
                sender.sendMessage("Invalid command");
            }
        }

        return true;
    }
}
