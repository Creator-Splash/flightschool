package com.a3v1k.flightSchool.platform.paper.command;

import com.a3v1k.flightSchool.domain.match.GameState;
import com.a3v1k.flightSchool.domain.player.GamePlayer;
import com.a3v1k.flightSchool.domain.team.Team;
import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Admin and debug FlightSchool commands rooted at {@code /fsh}.
 *
 * <p>Bundles game lifecycle controls (start/stop), setup helpers
 * (set-cannon, set-plane, team assignment), and the {@code /fsh debug ...}
 * subtree of in-development testing utilities. Gated by {@code fsh.admin}.
 * Player-facing commands live in {@link FsCommands}.</p>
 */
@Command("fsh")
@Permission("fsh.admin")
@RequiredArgsConstructor
public final class FsAdminCommands implements CommandHandler {

    private final FlightSchool plugin;

    /* == Lifecycle == */

    @Command("start")
    @CommandDescription("Start the FlightSchool game")
    public void start(CommandSender sender) {
        if (plugin.getGameManager().getGameState() != GameState.LOBBY) {
            sender.sendMessage(Component.text("The game has already started.",
                NamedTextColor.RED));
            return;
        }
        plugin.startGame();
        sender.sendMessage(Component.text(
            "The Flight School game has started! Role selection is now active.",
            NamedTextColor.GREEN));
    }

    @Command("stop")
    @CommandDescription("Stop the FlightSchool game and return to lobby")
    public void stop(CommandSender sender) {
        if (plugin.getGameManager().getGameState() == GameState.LOBBY) {
            sender.sendMessage(Component.text("The game is already in the lobby state.",
                NamedTextColor.RED));
            return;
        }
        plugin.stopGame();
        sender.sendMessage(Component.text("The Flight School game has stopped.",
            NamedTextColor.GREEN));
    }

    /* == Setup == */

    @Command("set-cannon <team>")
    @CommandDescription("Save your current location as a cannon spawn for the given team")
    public void setCannon(
        Player player,
        @Argument(value = "team", suggestions = "fsh-team-names") String teamName
    ) {
        Location location = player.getLocation();
        plugin.getConfigManager().addCannonLocation(teamName, location);
        player.sendMessage(Component.text(
            "Set successfully — cannon for team " + teamName,
            NamedTextColor.GREEN));
    }

    @Command("set-plane <team>")
    @CommandDescription("Save your current location as a plane spawn for the given team")
    public void setPlane(
        Player player,
        @Argument(value = "team", suggestions = "fsh-team-names") String teamName
    ) {
        Location location = player.getLocation();
        plugin.getConfigManager().addPlaneLocation(teamName, location);
        player.sendMessage(Component.text(
            "Set successfully — plane for team " + teamName,
            NamedTextColor.GREEN));
    }

    @Command("team <target> <team>")
    @CommandDescription("Assign a player to a team")
    public void assignTeam(
        CommandSender sender,
        @Argument("target") Player target,
        @Argument(value = "team", suggestions = "fsh-team-names") String teamName
    ) {
        Team team = plugin.getGameManager().getTeam(teamName);
        if (team == null) {
            sender.sendMessage(Component.text("Team " + teamName + " does not exist.",
                NamedTextColor.RED));
            return;
        }

        GamePlayer gamePlayer = plugin.getGameManager().getGamePlayer(target.getUniqueId());
        if (gamePlayer == null) {
            sender.sendMessage(Component.text(
                "Player " + target.getName() + " is not registered.",
                NamedTextColor.RED));
            return;
        }

        plugin.getGameManager().assignPlayerToTeam(target.getUniqueId(), team);
        sender.sendMessage(Component.text(
            "You've set " + target.getName() + " in the " + team.getName() + " team.",
            NamedTextColor.GREEN));
    }

    /* == Debug subtree == */

    @Command("debug spawnplane <team> <target>")
    @CommandDescription("Debug: spawn a plane for a team and player at the team's first plane spawn")
    public void debugSpawnPlane(
        CommandSender sender,
        @Argument(value = "team", suggestions = "fsh-team-names") String teamName,
        @Argument("target") Player target
    ) {
        String key = teamName.toLowerCase();
        Team team = plugin.getGameManager().getTeam(key);
        if (team == null) {
            sender.sendMessage(Component.text("Team not found.", NamedTextColor.RED));
            return;
        }

        List<Location> locs = plugin.getConfigManager().getPlaneLocations().get(key);
        if (locs == null || locs.isEmpty()) {
            sender.sendMessage(Component.text(
                "No plane spawn set for that team. Use /fsh set-plane first.",
                NamedTextColor.RED));
            return;
        }

        Location spawnLoc = locs.getFirst();
        plugin.getGameOrchestrator().spawnDelayedPlane(key, spawnLoc, target, 0);
        sender.sendMessage(Component.text(
                "Plane spawned for " + target.getName() + " on team " + key,
                NamedTextColor.GREEN));
    }

    @Command("debug cannons")
    @CommandDescription("Debug: spawn a turret near you and signal it to mount")
    public void debugCannons(Player player) {
        Location location = player.getLocation().add(5, 3, 5);

        MythicMob mob = MythicBukkit.inst().getMobManager()
            .getMythicMob("flightschool_turret_red").orElse(null);
        player.sendMessage(Component.text(
            MythicBukkit.inst().getMobManager().getMobNames().toString(),
            NamedTextColor.GRAY));
        if (mob == null) return;

        ActiveMob spawned = mob.spawn(BukkitAdapter.adapt(location), 1);

//        MythicBukkit.inst().getAPIHelper().castSkill(
//                spawned.getEntity().getBukkitEntity(),
//                "Turret_Mount_Mechanic",
//                player,
//                spawned.getEntity().getBukkitEntity().getLocation(),
//                null,
//                null,
//                1.0f
//        );

        // Wait for ModelEngine to finish building bones before signalling the mount
        plugin.getScheduler().runLater(() -> {
            if (spawned.isDead() || !player.isOnline()) return;
            spawned.signalMob(BukkitAdapter.adapt(player), "mountCannon");
        }, 10L);
    }

    @Command("debug planes")
    @CommandDescription("Debug: spawn a plane near you and signal it to mount")
    public void debugPlanes(Player player) {
        Location location = player.getLocation().add(5, 3, 5);

        MythicMob mob = MythicBukkit.inst().getMobManager()
            .getMythicMob("flightschool_plane_red").orElse(null);
        player.sendMessage(Component.text(
            MythicBukkit.inst().getMobManager().getMobNames().toString(),
            NamedTextColor.GRAY));
        if (mob == null) return;

        ActiveMob spawned = mob.spawn(BukkitAdapter.adapt(location), 1);

//        MythicBukkit.inst().getAPIHelper().castSkill(
//                spawned.getEntity().getBukkitEntity(),
//                "Turret_Mount_Mechanic",
//                player,
//                spawned.getEntity().getBukkitEntity().getLocation(),
//                null,
//                null,
//                1.0f
//        );

        // Wait for ModelEngine to finish building bones before signalling the mount
        plugin.getScheduler().runLater(() -> {
            if (spawned.isDead() || !player.isOnline()) return;
            spawned.signalMob(BukkitAdapter.adapt(player), "mountPlane");
        }, 10L);
    }

    @Command("debug pastemap")
    @CommandDescription("Debug: paste the map schematic at your current location")
    public void debugPasteMap(Player player) {
        plugin.getGameOrchestrator()
            .pasteMap(player.getLocation(), 8);
    }

    @Command("debug tabicon [team]")
    @CommandDescription("Debug: apply the TAB plugin team style — defaults to your own team")
    public void debugTabIcon(
        Player player,
        @Argument(value = "team", suggestions = "fsh-team-names") @Nullable String teamName
    ) {
        Team team = teamName != null
            ? plugin.getGameManager().getTeam(teamName.toLowerCase())
            : currentTeam(player);
        if (team == null) {
            player.sendMessage(Component.text(
                "No team found. Join a team first or pass one explicitly.",
                NamedTextColor.RED));
            return;
        }
        if (!plugin.getServer().getPluginManager().isPluginEnabled("TAB")) {
            player.sendMessage(Component.text("TAB must be installed for this test.",
                NamedTextColor.RED));
            return;
        }

        plugin.getTeamVisualManager().applyTabPluginStyle(player, team);
        player.sendMessage(Component.text(
            "Applied TAB icon test for team " + team.getName() + ".",
            NamedTextColor.GREEN));
    }

    @Command("debug tabreset")
    @CommandDescription("Debug: reset the TAB plugin style for yourself")
    public void debugTabReset(Player player) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("TAB")) {
            player.sendMessage(Component.text("TAB is not installed.", NamedTextColor.RED));
            return;
        }
        plugin.getTeamVisualManager().resetTabPluginStyle(player);
        player.sendMessage(Component.text("Reset your tab test name.", NamedTextColor.GREEN));
    }

    /* == Suggesters == */

    @Suggestions("fsh-team-names")
    public List<String> suggestTeams(CommandContext<CommandSender> ctx) {
        return plugin.getGameManager().getTeams().values().stream()
            .map(Team::getName)
            .toList();
    }

    /* == Helpers == */

    private @Nullable Team currentTeam(Player player) {
        GamePlayer gamePlayer = plugin.getGameManager().getGamePlayer(player.getUniqueId());
        return gamePlayer == null ? null : gamePlayer.getTeam();
    }
}
