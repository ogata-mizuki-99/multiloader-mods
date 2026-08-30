package com.ogatamizuki.guide.neoforge;

import com.ogatamizuki.guide.GuideAccess;
import com.ogatamizuki.guide.GuideItems;
import com.ogatamizuki.guide.client.GuideLibClient;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class GuideItemUseHandlerNeoForge {
    private GuideItemUseHandlerNeoForge() {}

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!GuideItems.isGuideItem(stack)) {
            return;
        }

        GuideAccess access = GuideItems.getAccess(stack);
        if (access == null) {
            return;
        }

        if (player.level().isClientSide()) {
            if (GuideAccess.KIND_CODEX.equals(access.kind())) {
                GuideLibClient.openCodex(null, null, access.themeId());
            } else if (GuideAccess.KIND_MANUAL.equals(access.kind()) && access.bookId() != null) {
                GuideLibClient.openBook(null, access.bookId(), access.themeId(), true);
            }
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
