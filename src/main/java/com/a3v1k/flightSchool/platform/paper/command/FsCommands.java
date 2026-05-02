package com.a3v1k.flightSchool.platform.paper.command;

import com.a3v1k.flightSchool.domain.match.GameState;
import com.a3v1k.flightSchool.domain.player.GamePlayer;
import com.a3v1k.flightSchool.domain.player.Role;
import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;

import java.util.List;
import java.util.logging.Level;

/**
 * Player-facing FlightSchool commands rooted at {@code /fsh}.
 *
 * <p>Holds gameplay subcommands available to any player. No permission gate.
 * Admin and debug subcommands live in {@link FsAdminCommands}, which shares the
 * same {@code /fsh} root — Cloud merges the trees on registration.</p>
 */
@Command("fsh")
@RequiredArgsConstructor
public final class FsCommands implements CommandHandler {

    private final FlightSchool plugin;

    @Command("playing-as <role>")
    @CommandDescription("Pick your role during role selection (flight or cannon)")
    public void playingAs(
        Player player,
        @Argument(value = "role", suggestions = "fsh-roles") String role
    ) {
        if (plugin.getGameManager().getGameState() != GameState.ROLE_SELECTION) {
            player.sendMessage(Component.text(
                "You can only select a role during the role selection phase.",
                NamedTextColor.RED));
            return;
        }

        GamePlayer gamePlayer = plugin.getGameManager().getGamePlayer(player.getUniqueId());
        if (gamePlayer == null) return;

        if (gamePlayer.getRole() != null) {
            player.sendMessage(Component.text("You have already selected a role.",
                NamedTextColor.RED));
            return;
        }

        Role roleToAssign;
        if (role.equalsIgnoreCase("flight")) {
            roleToAssign = Role.PLANE_PILOT;
        } else if (role.equalsIgnoreCase("cannon")) {
            roleToAssign = Role.CANNON_OPERATOR;
        } else {
            player.sendMessage(Component.text("Role must be 'flight' or 'cannon'.",
                NamedTextColor.RED));
            return;
        }

        if (!plugin.getGameManager().canAssignRole(gamePlayer.getTeam(), roleToAssign)) {
            player.sendMessage(Component.text(
                "The " + roleToAssign.toString().replace('_', ' ')
                    + " role is full for your team.",
                NamedTextColor.RED));
            return;
        }

        plugin.getGameManager().assignRole(player.getUniqueId(), roleToAssign);
        player.sendMessage(Component.text(
            "You have been assigned as a " + roleToAssign.toString().replace('_', ' ') + ".",
            NamedTextColor.GREEN));

        plugin.getLogger().log(Level.INFO, "{0} has been assigned as a {1}",
            new Object[]{player.getName(), roleToAssign});
    }

    @Suggestions("fsh-roles")
    public List<String> suggestRoles(CommandContext<CommandSender> ctx) {
        return List.of("flight", "cannon");
    }
}
