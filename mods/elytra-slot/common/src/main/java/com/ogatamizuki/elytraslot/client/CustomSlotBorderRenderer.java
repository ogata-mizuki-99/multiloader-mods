package com.ogatamizuki.elytraslot.client;

import com.ogatamizuki.elytraslot.ElytraSlot;
import com.ogatamizuki.elytraslot.FireworkSlot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.Slot;

public final class CustomSlotBorderRenderer {
    private CustomSlotBorderRenderer() {
    }

    public static boolean isCustomAttachmentSlot(Slot slot) {
        if (slot instanceof ElytraSlot || slot instanceof FireworkSlot) {
            return true;
        }
        return slot instanceof CustomSlotWrapper wrapper && wrapper.elytraSlot$isCustomAttachment();
    }

    /** Draw slot backgrounds before items/silhouettes are rendered (absolute screen coordinates). */
    public static void drawBackgrounds(GuiGraphicsExtractor gui, int leftPos, int topPos, Iterable<Slot> slots, int screenWidth, int screenHeight) {
        gui.enableScissor(0, 0, screenWidth, screenHeight);
        for (Slot slot : slots) {
            if (isCustomAttachmentSlot(slot) && slot.isActive()) {
                drawSlotBackground(gui, leftPos + slot.x - 1, topPos + slot.y - 1);
            }
        }
        gui.disableScissor();
    }

    private static void drawSlotBackground(GuiGraphicsExtractor gui, int x, int y) {
        gui.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        gui.fill(x, y, x + 18, y + 1, 0xFF373737);
        gui.fill(x, y, x + 1, y + 18, 0xFF373737);
        gui.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        gui.fill(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);
    }
}
