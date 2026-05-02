package com.a3v1k.flightSchool.platform.paper.game;

import com.a3v1k.flightSchool.application.game.BossbarManager;
import com.a3v1k.flightSchool.application.game.GameManager;
import com.a3v1k.flightSchool.application.game.LobbyManager;
import com.a3v1k.flightSchool.application.scheduler.Scheduler;
import com.a3v1k.flightSchool.domain.match.GameState;
import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import com.a3v1k.flightSchool.domain.player.GamePlayer;
import com.a3v1k.flightSchool.domain.player.Role;
import com.a3v1k.flightSchool.domain.team.Team;
import com.a3v1k.flightSchool.platform.paper.util.PlayerUtil;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public final class PaperLobbyManager implements LobbyManager {

    private final FlightSchool plugin;

    @Override
    public void startRoleSelection(List<UUID> playerIds) {
        List<Player> players = PlayerUtil.toOnlinePlayers(playerIds);

        this.plugin.getGameManager().setGameState(GameState.ROLE_SELECTION);
        this.plugin.enableLocatorBar();
        this.plugin.getLogger().info("Role selection has started.");

        assignTeams(players);
        teleportTeamsToRegions(players);

        ItemStack itemStack = new ItemStack(Material.RECOVERY_COMPASS, 1);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text("ᴘɪᴄᴋ ʏᴏᴜʀ ᴛᴇᴀᴍ", NamedTextColor.GOLD));
        itemStack.setItemMeta(itemMeta);

        for(Player player : players) {
            player.getInventory().clear();
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.setGameMode(GameMode.ADVENTURE);
            this.plugin.enableLocatorBarForPlayer(player);

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
        Scheduler.Task bossbarTask = plugin.getScheduler()
            .runRepeating(bossbarManager::update, 0L, 1L);

        plugin.getGameManager().setGameStartedAt(System.currentTimeMillis());

        plugin.getScheduler().runRepeating(t -> {
            if (bossbarManager.isFinished()) {
                t.cancel();
                bossbarTask.cancel();
                assignRolesRandomly(players);
                plugin.getGameOrchestrator().startGame(players);
            }
        }, 0L, 1L);
    }

    @Override
    public void startCinematic(List<UUID> playerIds) {
        List<Player> players = PlayerUtil.toOnlinePlayers(playerIds);

        this.plugin.getGameManager().setGameState(GameState.CINEMATIC);
        this.plugin.enableLocatorBar();

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



        // TODO: Switch it back to 40
        final int totalSeconds = 0;
        plugin.getScheduler().runRepeating(t -> {
            int remaining = totalSeconds - t.elapsedTicks();

            if (remaining <= 0) {
                t.cancel();
                startRoleSelection(playerIds);
                return;
            }

            if (remaining == 35) {
                Bukkit.dispatchCommand(
                    new ArrayList<>(Bukkit.getOnlinePlayers()).getFirst(),
                    "cinematic start flightmgr-demo all");

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(message);
                }
            }

            if (remaining == 25) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(message1);
                }
            }

            if (remaining == 15) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(message2);
                }
            }
        }, 0L, 20L);
    }

    private void assignRolesRandomly(List<Player> players) {
        GameManager gm = plugin.getGameManager();
        for (Player player : players) {
            GamePlayer gamePlayer = gm.getGamePlayer(player.getUniqueId());
            if (gamePlayer == null) continue;
            if (gamePlayer.getRole() != null) continue;

            Team team = gamePlayer.getTeam();
            if (team == null) continue;

            // Cannon-first: cannons are scarcer (2/team), so fill them before planes.
            if (gm.canAssignRole(team, Role.CANNON_OPERATOR)) {
                gm.assignRole(player.getUniqueId(), Role.CANNON_OPERATOR);
            } else if (gm.canAssignRole(team, Role.PLANE_PILOT)) {
                gm.assignRole(player.getUniqueId(), Role.PLANE_PILOT);
            } else {
                plugin.getLogger().warning("[Role Assignment] " + player.getName()
                    + " could not be assigned a role — team " + team.getName() + " is full.");
            }
        }
    }

    private void assignTeams(List<Player> allPlayers) {
        GameManager gm = plugin.getGameManager();

        List<Team> shuffledTeams = new ArrayList<>(gm.getTeams().values());
        Collections.shuffle(shuffledTeams);

        int unassignedIndex = 0;
        for (Player player : allPlayers) {
            GamePlayer gamePlayer = gm.getGamePlayer(player.getUniqueId());
            if (gamePlayer == null) continue;

            Team team;
            if (gamePlayer.getTeam() != null) {
                // Preserve existing assignment (e.g., seeded by FlightSchoolGameAdapter
                // or restored by resetRoundState's snapshot). Only newcomers consume the pool.
                team = gamePlayer.getTeam();
            } else {
                team = shuffledTeams.get(unassignedIndex % shuffledTeams.size());
                unassignedIndex++;
            }

            gm.assignPlayerToTeam(player.getUniqueId(), team);
            plugin.getLogger().info("[Team Assignment] "
                + player.getName() + " has been assigned to " + team.getName());
        }
    }

    private void teleportTeamsToRegions(List<Player> allPlayers) {
        GameManager gm = this.plugin.getGameManager();
        World gameWorld = Bukkit.getWorld("game-world");
        if (gameWorld == null) throw new IllegalStateException("Game world is not loaded!");

        // Get WorldGuard Container
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(gameWorld));

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
                this.plugin.getLogger()
                    .warning("Spawn region '" + regionName + "' does not exist for Team " + team.getName());
                continue;
            }

            // Calculate the center
            Location centerLoc = getRegionCenter(gameWorld, region);

            // Teleport all members of this team
            for (Player member : allPlayers) {
                if (member == null || !member.isOnline()) continue;
                if(gm.getGamePlayer(member.getUniqueId()).getTeam() != team) continue;
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
