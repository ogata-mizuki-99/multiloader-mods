package com.ogatamizuki.instantstructure;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class InstantBuilderItem extends Item {
    public InstantBuilderItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            InstantStructureServerOps.sendTemplatesToClient(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }
}
