package com.ogatamizuki.instantstructure;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class StructureMarkerItem extends Item {
    public StructureMarkerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player != null) {
            if (!level.isClientSide()) {
                if (player.isSecondaryUseActive()) {
                    InstantStructureServerOps.handleMarkerUndo(player);
                } else {
                    InstantStructureServerOps.handleMarkerClick(player, context.getClickedPos());
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
