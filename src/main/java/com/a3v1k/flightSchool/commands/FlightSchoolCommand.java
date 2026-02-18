package com.a3v1k.flightSchool.commands;

import com.a3v1k.flightSchool.FlightSchool;
import com.a3v1k.flightSchool.game.GameState;
import com.a3v1k.flightSchool.player.GamePlayer;
import com.a3v1k.flightSchool.player.Role;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.mount.controller.MountControllerTypes;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class FlightSchoolCommand implements CommandExecutor {

    private final FlightSchool plugin;

    public FlightSchoolCommand(FlightSchool plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("fsh")) {
                if (args[0].equalsIgnoreCase("start")) {
                    if (plugin.getGameManager().getGameState() != GameState.LOBBY) {
                        sender.sendMessage("The game has already started.");
                        return true;
                    }
                    plugin.startGame();
                    sender.sendMessage("The Flight School game has started! Role selection is now active.");
                    return true;
                }

                if (!(sender instanceof Player)) {
                    sender.sendMessage("This command can only be run by a player.");
                    return true;
                }
                Player player = (Player) sender;

                if (args[0].equalsIgnoreCase("set-cannon")) {
                    Location location = player.getLocation();
                    this.plugin.getConfigManager().addCannonLocation(args[1], location);
                    player.sendMessage("set successfully - cannon for team " + args[1]);
                }

                if (args[0].equalsIgnoreCase("set-plane")) {
                    Location location = player.getLocation();
                    this.plugin.getConfigManager().addPlaneLocation(args[1], location);
                    player.sendMessage("set successfully - plane for team " + args[1]);
                }

                if (args[0].equalsIgnoreCase("playing-as")) {
                    if (plugin.getGameManager().getGameState() != GameState.ROLE_SELECTION) {
                        player.sendMessage("You can only select a role during the role selection phase.");
                        return true;
                    }
                    GamePlayer gamePlayer = plugin.getGameManager().getGamePlayer(player);
                    if (gamePlayer.getRole() != null) {
                        player.sendMessage("You have already selected a role.");
                        return true;
                    }

                    if (args.length > 1) {
                        Role roleToAssign = null;
                        if (args[1].equalsIgnoreCase("Flight")) {
                            roleToAssign = Role.PLANE_PILOT;
                        } else if (args[1].equalsIgnoreCase("Cannon")) {
                            roleToAssign = Role.CANNON_OPERATOR;
                        }

                        if (roleToAssign != null) {
                            if (plugin.getGameManager().canAssignRole(gamePlayer.getTeam(), roleToAssign)) {
                                plugin.getGameManager().assignRole(player, roleToAssign);
                                player.sendMessage("You have been assigned as a " + roleToAssign.toString().replace('_', ' ') + ".");
                                plugin.getLogger().log(Level.INFO, "{0} has been assigned as a {1}", new Object[]{player.getName(), roleToAssign});
                            } else {
                                player.sendMessage("The " + roleToAssign.toString().replace('_', ' ') + " role is full for your team.");
                            }
                        }
                    }
                    return true;
                }
            }

        return false;
    }
}
