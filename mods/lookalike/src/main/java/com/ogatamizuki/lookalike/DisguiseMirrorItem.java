package com.ogatamizuki.lookalike;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

public class DisguiseMirrorItem extends Item {
    /** 実質的に無限（弓と同じ慣習的な値）。ラジアルメニューの開閉はクライアント側で制御する。 */
    public static final int MAX_USE_TICKS = 72000;

    public DisguiseMirrorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_USE_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide()) {
            com.ogatamizuki.lookalike.client.ClientAccess.cancelRadialMenu();
        }
        return true;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand hand) {
        if (!player.isSecondaryUseActive()) {
            return super.interactLivingEntity(stack, player, interactionTarget, hand);
        }
        if (!(interactionTarget instanceof Player targetPlayer) || targetPlayer.getUUID().equals(player.getUUID())) {
            return InteractionResult.PASS;
        }

        if (!player.level().isClientSide()) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            ServerPlayer serverTarget = (ServerPlayer) targetPlayer;

            LookalikeAttachments.ScanHistory history = serverPlayer.getData(LookalikeAttachments.SCAN_HISTORY);
            history.addEntry(
                    serverTarget.getUUID(),
                    DisguiseManager.getStoredGameProfile(serverTarget).name()
            );
            serverPlayer.setData(LookalikeAttachments.SCAN_HISTORY, history);
            LookalikeMod.syncScanHistory(serverPlayer);
            serverPlayer.sendSystemMessage(Component.translatable(
                    "lookalike.message.scan_success", DisguiseManager.getStoredGameProfile(serverTarget).name()));
        }
        return InteractionResult.SUCCESS;
    }
}
