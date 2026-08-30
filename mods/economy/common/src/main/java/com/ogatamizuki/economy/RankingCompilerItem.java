package com.ogatamizuki.economy;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

public class RankingCompilerItem extends Item {
    public RankingCompilerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            // GM/OP権限チェック
            if (serverPlayer.createCommandSourceStack().permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)) {
                EconomyCommands.compileRanking(serverPlayer.createCommandSourceStack());
            } else {
                player.sendSystemMessage(Component.literal("§c[エラー] このアイテムを使用する権限（管理者権限）がありません。"));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
