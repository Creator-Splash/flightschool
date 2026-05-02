package com.a3v1k.flightSchool.platform.paper.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.meta.CommandMeta;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public final class CommandRegistrar extends LegacyPaperCommandManager<CommandSender> {

    private final AnnotationParser<CommandSender> annotationParser;

    public CommandRegistrar(@NotNull Plugin plugin) throws Exception {
        super(
            plugin,
            ExecutionCoordinator.simpleCoordinator(),
            SenderMapper.identity()
        );

        this.annotationParser = new AnnotationParser<>(
                this,
                CommandSender.class,
                params -> CommandMeta.empty()
        );

        this.registerDefaultExceptionHandlers(
                triplet -> {
                    final CommandSender sender = this.senderMapper().reverse(triplet.first().sender());
                    final String message = triplet.first().formatCaption(triplet.second(), triplet.third());
                    sender.sendMessage(Component.text(message, NamedTextColor.RED));
                },
                pair -> plugin.getLogger().log(Level.SEVERE, pair.first(), pair.second())
        );
    }

    public void registerAnnotated(@NotNull CommandHandler command) {
        this.annotationParser.parse(command);
    }
}
