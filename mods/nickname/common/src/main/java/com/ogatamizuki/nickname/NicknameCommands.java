package com.ogatamizuki.nickname;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.UUID;
import java.util.function.BiConsumer;

public class NicknameCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                BiConsumer<UUID, String> onNicknameChanged,
                                Runnable onClearAll) {
        dispatcher.register(
                Commands.literal("nickname")
                        .requires(source -> true) // OP権限不要、全プレイヤー実行可能
                        .then(Commands.literal("clear")
                                .executes(context -> {
                                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                        changeNickname(player, "", onNicknameChanged);
                                        return 1;
                                    }
                                    return 0;
                                }))
                        .then(Commands.literal("clearall")
                                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) // OP権限が必要
                                .executes(context -> {
                                    MinecraftServer server = context.getSource().getServer();
                                    NicknameStorage.clear();
                                    NicknameStorage.saveAsync(server);

                                    // 全プレイヤーの表示名およびTABリスト名をリフレッシュ
                                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                                        NicknamePlatform.refresh(player);
                                    }

                                    onClearAll.run();

                                    context.getSource().sendSuccess(
                                            () -> Component.translatable("nickname.message.cleared_all").withStyle(ChatFormatting.GREEN),
                                            true);
                                    return 1;
                                }))
                        .then(Commands.literal("set")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                                String newName = StringArgumentType.getString(context, "name");
                                                changeNickname(player, newName, onNicknameChanged);
                                                return 1;
                                            }
                                            return 0;
                                        })))
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(context -> {
                                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                        String newName = StringArgumentType.getString(context, "name");
                                        changeNickname(player, newName, onNicknameChanged);
                                        return 1;
                                    }
                                    return 0;
                                }))
                        .executes(context -> {
                            if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                // 引数なしの場合はニックネーム解除
                                changeNickname(player, "", onNicknameChanged);
                                return 1;
                            }
                            return 0;
                        }));
    }

    public static void changeNickname(ServerPlayer player, String newName, BiConsumer<UUID, String> onNicknameChanged) {
        UUID uuid = player.getUUID();
        NicknameValidation.Result validation = NicknameValidation.validate(newName);
        if (!validation.accepted()) {
            String errorKey = validation.errorKey().orElse("nickname.message.invalid");
            if (validation.errorArg() > 0) {
                player.sendSystemMessage(
                        Component.translatable(errorKey, validation.errorArg()).withStyle(ChatFormatting.RED));
            } else {
                player.sendSystemMessage(Component.translatable(errorKey).withStyle(ChatFormatting.RED));
            }
            return;
        }

        String nameToSet = validation.sanitized();

        NicknameStorage.setNickname(uuid, nameToSet);
        if (nameToSet.isEmpty()) {
            player.sendSystemMessage(Component.translatable("nickname.message.reset").withStyle(ChatFormatting.GREEN));
        } else {
            player.sendSystemMessage(Component.translatable("nickname.message.set", nameToSet).withStyle(ChatFormatting.GREEN));
        }

        NicknamePlatform.refresh(player);

        // サーバー側の保存データを更新
        var server = player.level().getServer();
        if (server != null) {
            NicknameStorage.saveAsync(server);
        }

        onNicknameChanged.accept(uuid, nameToSet);
    }
}
