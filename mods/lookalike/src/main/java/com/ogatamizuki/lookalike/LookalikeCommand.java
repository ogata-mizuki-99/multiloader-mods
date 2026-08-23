package com.ogatamizuki.lookalike;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.ogatamizuki.lookalike.cast.CastEffectTemplate;
import com.ogatamizuki.lookalike.cast.CastManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.Locale;

public class LookalikeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("lookalike")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.literal("disguise")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                                context.getSource().getOnlinePlayerNames(),
                                                builder
                                        ))
                                        .then(Commands.argument("targetName", StringArgumentType.word())
                                                .executes(context -> disguise(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        StringArgumentType.getString(context, "targetName"),
                                                        0,
                                                        0,
                                                        CastEffectTemplate.NONE
                                                ))
                                                .then(Commands.argument("duration", IntegerArgumentType.integer(0))
                                                        .executes(context -> disguise(
                                                                context.getSource(),
                                                                EntityArgument.getPlayer(context, "player"),
                                                                StringArgumentType.getString(context, "targetName"),
                                                                IntegerArgumentType.getInteger(context, "duration"),
                                                                0,
                                                                CastEffectTemplate.NONE
                                                        ))
                                                        .then(Commands.argument("castTime", IntegerArgumentType.integer(0))
                                                                .executes(context -> disguise(
                                                                        context.getSource(),
                                                                        EntityArgument.getPlayer(context, "player"),
                                                                        StringArgumentType.getString(context, "targetName"),
                                                                        IntegerArgumentType.getInteger(context, "duration"),
                                                                        IntegerArgumentType.getInteger(context, "castTime"),
                                                                        CastEffectTemplate.WITCH_SMOKE
                                                                ))
                                                                .then(Commands.argument("effect", StringArgumentType.word())
                                                                        .suggests((context, builder) -> {
                                                                            for (CastEffectTemplate template : CastEffectTemplate.values()) {
                                                                                builder.suggest(template.name().toLowerCase(Locale.ROOT));
                                                                            }
                                                                            return builder.buildFuture();
                                                                        })
                                                                        .executes(context -> disguise(
                                                                                context.getSource(),
                                                                                EntityArgument.getPlayer(context, "player"),
                                                                                StringArgumentType.getString(context, "targetName"),
                                                                                IntegerArgumentType.getInteger(context, "duration"),
                                                                                IntegerArgumentType.getInteger(context, "castTime"),
                                                                                CastEffectTemplate.fromName(
                                                                                        StringArgumentType.getString(context, "effect"))
                                                                        ))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("clear")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                                context.getSource().getOnlinePlayerNames(),
                                                builder
                                        ))
                                        .executes(context -> clear(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player")
                                        ))
                                )
                        )
                        .then(Commands.literal("shadow")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> shadow(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                0
                                        ))
                                        .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                                .executes(context -> shadow(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        IntegerArgumentType.getInteger(context, "duration")
                                                ))
                                        )
                                )
                        )
        );
    }

    private static int disguise(
            CommandSourceStack source,
            ServerPlayer player,
            String targetName,
            int durationSeconds,
            int castTimeSeconds,
            CastEffectTemplate effect
    ) {
        CastManager.getInstance().startCastAsName(player, targetName, durationSeconds, castTimeSeconds, effect);
        if (castTimeSeconds > 0) {
            source.sendSuccess(() -> Component.translatable(
                    "commands.lookalike.disguise.casting",
                    player.getScoreboardName(),
                    targetName,
                    castTimeSeconds,
                    effect.name()), true);
        } else {
            source.sendSuccess(() -> Component.translatable(
                    "commands.lookalike.disguise.success", player.getScoreboardName(), targetName), true);
        }
        return 1;
    }

    private static int clear(CommandSourceStack source, ServerPlayer player) {
        boolean wasCasting = CastManager.getInstance().cancelCastIfActive(player);
        if (DisguiseManager.getInstance().undisguise(player) || wasCasting) {
            source.sendSuccess(() -> Component.translatable(
                    "commands.lookalike.clear.success", player.getScoreboardName()), true);
            return 1;
        }
        source.sendFailure(Component.translatable("commands.lookalike.clear.failure", player.getScoreboardName()));
        return 0;
    }

    private static int shadow(CommandSourceStack source, ServerPlayer player, int durationSeconds) {
        ShadowAppearanceManager.getInstance().enableShadow(player, durationSeconds);
        source.sendSuccess(() -> Component.translatable(
                "commands.lookalike.shadow.success", player.getScoreboardName()), true);
        return 1;
    }
}
