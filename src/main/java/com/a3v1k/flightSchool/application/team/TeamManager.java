package com.a3v1k.flightSchool.application.team;

import com.a3v1k.flightSchool.domain.team.Team;
import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import com.a3v1k.flightSchool.application.game.GameManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TeamManager {

    private final FlightSchool plugin;
    private final LuckPerms luckPerms;

    public TeamManager() {
        this.plugin = FlightSchool.getInstance();
        this.luckPerms = LuckPermsProvider.get();
    }

    public void assignTeamsAndTeleport() {
        GameManager gameManager = plugin.getGameManager();
        List<Player> playersToAssign = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                String primaryGroup = user.getPrimaryGroup();
                if (!primaryGroup.equalsIgnoreCase("Team") && !primaryGroup.equalsIgnoreCase("Owner")) {
                    playersToAssign.add(player);
                }
            }
        }

        Collections.shuffle(playersToAssign);

        List<Team> teams = new ArrayList<>(gameManager.getTeams().values());
        int teamIndex = 0;
        for (Player player : playersToAssign) {
            Team team = teams.get(teamIndex);
            gameManager.assignPlayerToTeam(player, team);
            teleportPlayerToSpawn(player, team);
            teamIndex = (teamIndex + 1) % teams.size();
        }
    }

    public void teleportPlayerToSpawn(Player player, Team team) {
        World world = Bukkit.getWorld("minecraft:game-world");
        if (world == null) {
            plugin.getLogger().severe("World 'minecraft:game-world' not found!");
            return;
        }

        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (regionManager == null) {
            plugin.getLogger().severe("Region manager for world 'minecraft:game-world' not found!");
            return;
        }

        ProtectedRegion region = regionManager.getRegion(team.getSpawnRegionName());
        if (region == null) {
            plugin.getLogger().severe("Region '" + team.getSpawnRegionName() + "' not found!");
            return;
        }

        BlockVector3 center = region.getMaximumPoint().subtract(region.getMinimumPoint()).divide(2).add(region.getMinimumPoint());
        Location spawnLocation = new Location(world, center.x(), center.y(), center.z());
        player.teleport(spawnLocation);
    }
}
