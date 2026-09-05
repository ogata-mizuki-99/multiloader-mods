package com.ogatamizuki.guide.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

public final class GuideItemTooltips {
    private GuideItemTooltips() {}

    public static boolean isMouseOver(int mouseX, int mouseY, int slotX, int slotY, int slotSize) {
        return mouseX >= slotX && mouseX < slotX + slotSize && mouseY >= slotY && mouseY < slotY + slotSize;
    }

    public static void renderItemTooltip(
            GuiGraphicsExtractor gui,
            Font font,
            ItemStack stack,
            int mouseX,
            int mouseY,
            int slotX,
            int slotY,
            int slotSize
    ) {
        if (stack.isEmpty() || !isMouseOver(mouseX, mouseY, slotX, slotY, slotSize)) {
            return;
        }
        gui.setTooltipForNextFrame(font, stack, mouseX, mouseY);
    }
}
