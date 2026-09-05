package com.ogatamizuki.guide.fabric;

import com.ogatamizuki.guide.GuideAccess;
import com.ogatamizuki.guide.GuideItems;
import com.ogatamizuki.guide.client.GuideLibClient;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class GuideItemUseHandlerFabric {
    private GuideItemUseHandlerFabric() {}

    public static void register() {
        UseItemCallback.EVENT.register(GuideItemUseHandlerFabric::onUseItem);
    }

    private static InteractionResult onUseItem(Player player, Level level, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!GuideItems.isGuideItem(stack)) {
            return InteractionResult.PASS;
        }

        GuideAccess access = GuideItems.getAccess(stack);
        if (access == null) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            if (GuideAccess.KIND_CODEX.equals(access.kind())) {
                GuideLibClient.openCodex(null, null, access.themeId());
            } else if (GuideAccess.KIND_MANUAL.equals(access.kind()) && access.bookId() != null) {
                GuideLibClient.openBook(null, access.bookId(), access.themeId(), true);
            }
        }

        return InteractionResult.SUCCESS;
    }
}
