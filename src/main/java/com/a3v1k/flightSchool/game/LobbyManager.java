package com.a3v1k.flightSchool.game;

import com.a3v1k.flightSchool.FlightSchool;
import com.a3v1k.flightSchool.player.GamePlayer;
import com.a3v1k.flightSchool.player.Role;
import com.a3v1k.flightSchool.team.Team;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LobbyManager {

    private final FlightSchool plugin;
    private int countdown = 20;

    public LobbyManager() {
        this.plugin = FlightSchool.getInstance();
    }

    public void startRoleSelection(List<Player> players) {
        this.plugin.getGameManager().setGameState(GameState.ROLE_SELECTION);
        this.plugin.getLogger().info("Role selection has started.");

        assignTeams(players);
        teleportTeamsToRegions(players);

        ItemStack itemStack = new ItemStack(Material.RECOVERY_COMPASS, 1);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(ChatColor.GOLD + "ᴘɪᴄᴋ ʏᴏᴜʀ ᴛᴇᴀᴍ");
        itemStack.setItemMeta(itemMeta);

        for(Player player : players) {
            player.getInventory().clear();

            player.getInventory().setItem(4, itemStack);
        }

        Component message =
                Component.text("════════════════════════════════", NamedTextColor.DARK_GREEN)
                        .append(Component.newline())
                        .append(
                                Component.text("Each team has a blimp, consisting of 2 cannons and 3 planes.", NamedTextColor.GREEN)
                        )
                        .append(Component.newline())
                        .append(Component.newline())
                        .append(
                                Component.text("Please select your roles during this time using the compass.", NamedTextColor.GOLD)
                        )
                        .append(Component.newline())
                        .append(Component.text("════════════════════════════════", NamedTextColor.DARK_GREEN));

        for(Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }

        BossbarManager bossbarManager = new BossbarManager(20 * 20);
        bossbarManager.runTaskTimer(this.plugin, 0, 1);

        FlightSchool.getInstance().getGameManager().setGameStartedAt(System.currentTimeMillis());

        new BukkitRunnable() {
            @Override
            public void run() {
                if(bossbarManager.isCancelled()) {
                    this.cancel();

                    // If some players have not been assigned a team, fill them in. 2 CANNONs, 3 PLANES ONLY.
                    assignRolesRandomly(players);

                    // Change to GAME
                    plugin.getGameManager().startGame(players);
                }
            }
        }.runTaskTimer(this.plugin, 0, 1);
    }

    public void startCinematic(List<Player> players) {
        this.plugin.getGameManager().setGameState(GameState.CINEMATIC);

        for(Player player : Bukkit.getOnlinePlayers()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20*5, 10));
        }

        Component message =
                Component.text("════════════════════════════════", NamedTextColor.DARK_GREEN)
                        .append(Component.newline())
                        .append(
                                Component.text("Welcome to Flight School!", NamedTextColor.RED)
                        )
                        .append(Component.newline())
                        .append(Component.newline())
                        .append(
                                Component.text("Each team has a blimp, consisting of 2 cannons and 3 planes.", NamedTextColor.GOLD)
                        )
                        .append(Component.newline())
                        .append(Component.text("════════════════════════════════", NamedTextColor.DARK_GREEN));

        Component message1 =
                Component.text("════════════════════════════════", NamedTextColor.DARK_GREEN)
                        .append(Component.newline())
                        .append(
                                Component.text("Your team of 5 would get the opportunity to choose 2 cannon players and 3 planes.", NamedTextColor.RED)
                        )
                        .append(Component.newline())
                        .append(Component.newline())
                        .append(
                                Component.text("As a plane, you must try to shoot down your enemy's cannons.", NamedTextColor.GOLD)
                        )
                        .append(Component.newline())
                        .append(Component.text("════════════════════════════════", NamedTextColor.DARK_GREEN));

        Component message2 =
                Component.text("════════════════════════════════", NamedTextColor.DARK_GREEN)
                        .append(Component.newline())
                        .append(
                                Component.text("The cannons must protect the blimp. If the cannons fall, the team's blimp is eliminated.", NamedTextColor.RED)
                        )
                        .append(Component.newline())
                        .append(Component.newline())
                        .append(
                                Component.text("Planes have one last stand after their respective blimps fall.", NamedTextColor.GOLD)
                        )
                        .append(Component.newline())
                        .append(Component.text("════════════════════════════════", NamedTextColor.DARK_GREEN));



        // Start the cinematic
        final int[] countdown = {5}; // TODO: Switch it back to 40
        new BukkitRunnable() {
            @Override
            public void run() {
                if(countdown[0] <= 0) {
                    this.cancel();

                    startRoleSelection(players);
                }

                if(countdown[0] == 35) {
                    Bukkit.dispatchCommand(new ArrayList<>(Bukkit.getOnlinePlayers()).get(0), "cinematic start flightmgr-demo all");

                    for(Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(message);
                    }
                }

                if(countdown[0] == 25) {

                    for(Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(message1);
                    }
                }

                if(countdown[0] == 15) {
                    for(Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(message2);
                    }
                }
                countdown[0] -= 1;
            }
        }.runTaskTimer(this.plugin, 0, 20);
    }

    private void assignRolesRandomly(List<Player> players) {
        for(Player player : players) {
            GamePlayer gamePlayer = this.plugin.getGameManager().getGamePlayer(player);
            if(gamePlayer == null) continue;

            if(gamePlayer.getRole() != null) continue;

            // Review team numbers.
            Team team = gamePlayer.getTeam();

            if(team == null) continue;

            long cannonCount = this.plugin.getGameManager().getRoleCount(team, Role.CANNON_OPERATOR);
            long planeCount = this.plugin.getGameManager().getRoleCount(team, Role.PLANE_PILOT);

            if(!(String.valueOf(cannonCount).equals(String.valueOf(2)))) {
                long allowedCannonCount = 2 - cannonCount;

                this.plugin.getGameManager().assignRole(player, Role.CANNON_OPERATOR);
            }

            if(!(String.valueOf(planeCount).equals(String.valueOf(3)))) {
                long allowedPlaneCount = 3 - planeCount;

                this.plugin.getGameManager().assignRole(player, Role.PLANE_PILOT);
            }
        }
    }

    private void assignTeams(List<Player> allPlayers) {
        GameManager gm = this.plugin.getGameManager();
        // Shuffle teams, assign first 5 to shuffled, then so on
        List<Team> shuffledTeams = List.copyOf(gm.getTeams().values());
        for(int i = 0; i < allPlayers.size(); i++) {
            Team team = shuffledTeams.get(i % shuffledTeams.size());
            GamePlayer gamePlayer = gm.getGamePlayer(allPlayers.get(i));
            if(gamePlayer.getTeam() != null) team = gamePlayer.getTeam();
            gamePlayer.setTeam(team);
            if(!team.getMembers().contains(allPlayers.get(i).getUniqueId())) team.addMember(allPlayers.get(i));
            this.plugin.getLogger().info("[Team Assignment] " + allPlayers.get(i).getName() + " has been assigned to " + team.getName());
        }

        // See how many empty teams
        int emptyTeams = 0;
        for(Team team : gm.getTeams().values()) {
            if(team.getMembers().isEmpty()) {
                emptyTeams++;
            }
        }
        int totalTeams = shuffledTeams.size() - emptyTeams;
    }

    private void teleportTeamsToRegions(List<Player> allPlayers) {
        GameManager gm = this.plugin.getGameManager();
        org.bukkit.World gameWorld = Bukkit.getWorld("game-world");

        // Get WorldGuard Container
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(gameWorld));

        if (regions == null) {
            this.plugin.getLogger().severe("WorldGuard regions not found for game world!");
            return;
        }

        // Get all regions
        for (Team team : gm.getTeams().values()) {
            String regionName = team.getSpawnRegionName();

            // Get the WorldGuard region
            ProtectedRegion region = regions.getRegion(regionName);
            if (region == null) {
                this.plugin.getLogger().warning("Spawn region '" + regionName + "' does not exist for Team " + team.getName());
                continue;
            }

            // Calculate the center
            Location centerLoc = getRegionCenter(gameWorld, region);

            // Teleport all members of this team
            for (Player member : allPlayers) {
                if(gm.getGamePlayer(member).getTeam() != team) continue;
                member.teleport(centerLoc);
            }
        }
    }

    private Location getRegionCenter(org.bukkit.World world, ProtectedRegion region) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        // Calculate middle coordinates
        double x = (min.x() + max.x()) / 2.0 + 0.5; // +0.5 to center on the block
        double z = (min.z() + max.z()) / 2.0 + 0.5;

        // For Y, we usually want the FLOOR of the region, not the floating center
        // If you want them to float in the middle, use (minY + maxY) / 2.0
        double y = (min.y() + max.y()) / 2.0;

        return new Location(world, x, y, z);
    }
}
