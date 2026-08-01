package com.ogatamizuki.economy;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class MobileTerminalItem extends Item {
    public MobileTerminalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            if (EconomyMod.isEconomyReady()) {
                ClientAccess.openStockTradeScreen();
            } else {
                player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal("§c[経済] 経済データの同期が完了していないため、取引端末を利用できません。"));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
