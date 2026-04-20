package com.a3v1k.flightSchool.commands;

import com.a3v1k.flightSchool.FlightSchool;
import com.a3v1k.flightSchool.player.GamePlayer;
import com.a3v1k.flightSchool.team.Team;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.mount.controller.MountControllerTypes;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
public class TempCommands implements CommandExecutor {

    private final FlightSchool plugin;

    public TempCommands(FlightSchool flightSchool) {
        this.plugin = flightSchool;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {
        if(!commandSender.hasPermission("flightSchool.admin")) return true;

        if (command.getName().equalsIgnoreCase("fsh-test")) {
            if (!(commandSender instanceof Player player)) {
                commandSender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }

            if (strings.length == 0) {
                player.sendMessage(ChatColor.YELLOW + "Usage: /fsh-test <cannons|planes|pastemap|tabicon|tabreset> [team]");
                return true;
            }

            if (strings[0].equalsIgnoreCase("cannons")) {
                Location location = player.getLocation().add(5, 3, 5);

                MythicMob mob = MythicBukkit.inst().getMobManager().getMythicMob("flightschool_turret_red").orElse(null);
                player.sendMessage(MythicBukkit.inst().getMobManager().getMobNames().toString());
                if(mob != null){
                    // spawns mob
                    ActiveMob knight = mob.spawn(BukkitAdapter.adapt(location),1);

                    // get mob as bukkit entity
                    Entity entity = knight.getEntity().getBukkitEntity();

//                    MythicBukkit.inst().getAPIHelper().castSkill(
//                            knight.getEntity().getBukkitEntity(),
//                            "Turret_Mount_Mechanic",
//                            player,
//                            knight.getEntity().getBukkitEntity().getLocation(),
//                            null,
//                            null,
//                            1.0f
//                    );
                    // 3. WAIT for ModelEngine to build the bones
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (knight.isDead() || !player.isOnline()) return;

                            // Now that the 'seat' bone exists, this signal will work
                            knight.signalMob(BukkitAdapter.adapt(player), "mountCannon");
                        }
                    }.runTaskLater(this.plugin, 10L); // Delay 10 ticks (0.5 seconds)
                }

                return true;
            }

            if (strings[0].equalsIgnoreCase("planes")) {
                Location location = player.getLocation().add(5, 3, 5);

                MythicMob mob = MythicBukkit.inst().getMobManager().getMythicMob("flightschool_plane_red").orElse(null);
                player.sendMessage(MythicBukkit.inst().getMobManager().getMobNames().toString());
                if(mob != null){
                    // spawns mob
                    ActiveMob knight = mob.spawn(BukkitAdapter.adapt(location),1);

                    // get mob as bukkit entity
                    Entity entity = knight.getEntity().getBukkitEntity();

//                    MythicBukkit.inst().getAPIHelper().castSkill(
//                            knight.getEntity().getBukkitEntity(),
//                            "Turret_Mount_Mechanic",
//                            player,
//                            knight.getEntity().getBukkitEntity().getLocation(),
//                            null,
//                            null,
//                            1.0f
//                    );
                    // 3. WAIT for ModelEngine to build the bones
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (knight.isDead() || !player.isOnline()) return;

                            // Now that the 'seat' bone exists, this signal will work
                            knight.signalMob(BukkitAdapter.adapt(player), "mountPlane");
                        }
                    }.runTaskLater(this.plugin, 10L); // Delay 10 ticks (0.5 seconds)
                }

                return true;
            }

            if (strings[0].equalsIgnoreCase("pastemap")) {
                this.plugin.getGameManager().pasteMap(player.getLocation(), 8);
                return true;
            }

            if (strings[0].equalsIgnoreCase("tabicon")) {
                Team team = resolveTargetTeam(player, strings);
                if (team == null) {
                    player.sendMessage(ChatColor.RED + "No team found. Join a team first or pass one explicitly.");
                    return true;
                }

                if (!plugin.getServer().getPluginManager().isPluginEnabled("TAB")) {
                    player.sendMessage(ChatColor.RED + "TAB must be installed for this test.");
                    return true;
                }

                plugin.getTeamVisualManager().applyTabPluginStyle(player, team);
                player.sendMessage(ChatColor.GREEN + "Applied TAB icon test for team " + team.getName() + ".");
                return true;
            }

            if (strings[0].equalsIgnoreCase("tabreset")) {
                if (!plugin.getServer().getPluginManager().isPluginEnabled("TAB")) {
                    player.sendMessage(ChatColor.RED + "TAB is not installed.");
                    return true;
                }

                plugin.getTeamVisualManager().resetTabPluginStyle(player);
                player.sendMessage(ChatColor.GREEN + "Reset your tab test name.");
                return true;
            }
            return true;
        }

        return false;
    }

    private Team resolveTargetTeam(Player player, String[] args) {
        if (args.length >= 2) {
            return plugin.getGameManager().getTeam(args[1].toLowerCase());
        }

        GamePlayer gamePlayer = plugin.getGameManager().getGamePlayer(player);
        return gamePlayer == null ? null : gamePlayer.getTeam();
    }
}
