package com.ogatamizuki.deconstructor;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class EnchantmentManagerScreen extends AbstractContainerScreen<EnchantmentManagerMenu> {
    private static final int WIN_WIDTH = 176;
    private static final int WIN_HEIGHT = 166;

    public EnchantmentManagerScreen(EnchantmentManagerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, WIN_WIDTH, WIN_HEIGHT);
        this.inventoryLabelY = 72;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;

        // ---- Outer Bevel ----
        drawBevel(gui, x, y, x + this.imageWidth, y + this.imageHeight, false);

        // ---- Input Slots ----
        // Slot 0 (44, 35)
        drawBevel(gui, x + 43, y + 34, x + 61, y + 52, true);
        drawSlotBorder(gui, x + 43, y + 34);

        // Slot 1 (80, 35)
        drawBevel(gui, x + 79, y + 34, x + 97, y + 52, true);
        drawSlotBorder(gui, x + 79, y + 34);

        // ---- Output Slot (134, 35) ----
        drawBevel(gui, x + 133, y + 34, x + 151, y + 52, true);
        drawSlotBorder(gui, x + 133, y + 34);

        // ---- Arrow symbol or plus symbol ----
        // Plus symbol between Slot 0 and Slot 1
        gui.text(this.font, "+", x + 68, y + 38, 0x80FFFFFF, false);
        // Arrow symbol between Slot 1 and Output Slot 2
        gui.text(this.font, "=>", x + 109, y + 38, 0x80FFFFFF, false);

        // ---- Player Inventory ----
        drawBevel(gui, x + 7, y + 83, x + 169, y + 137, true);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                drawSlotBorder(gui, x + 7 + c * 18, y + 83 + r * 18);
            }
        }

        // ---- Hotbar ----
        drawBevel(gui, x + 7, y + 141, x + 169, y + 159, true);
        for (int c = 0; c < 9; c++) {
            drawSlotBorder(gui, x + 7 + c * 18, y + 141);
        }

        super.extractContents(gui, mouseX, mouseY, partialTick);
    }

    private void drawBevel(GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2, boolean sunken) {
        int strokeColor = sunken ? 0x25FFFFFF : 0x40FFFFFF;
        int bgColor     = sunken ? 0xFF07090E : 0xFF14161E;
        gui.fill(x1, y1, x2, y2, bgColor);
        gui.fill(x1, y1, x2, y1 + 1, strokeColor);
        gui.fill(x1, y1, x1 + 1, y2, strokeColor);
        gui.fill(x1, y2 - 1, x2, y2, strokeColor);
        gui.fill(x2 - 1, y1, x2, y2, strokeColor);
    }

    private void drawSlotBorder(GuiGraphicsExtractor gui, int slotX, int slotY) {
        int strokeColor = 0x30FFFFFF;
        gui.fill(slotX,      slotY,      slotX + 18, slotY + 1,  strokeColor);
        gui.fill(slotX,      slotY,      slotX + 1,  slotY + 18, strokeColor);
        gui.fill(slotX,      slotY + 17, slotX + 18, slotY + 18, strokeColor);
        gui.fill(slotX + 17, slotY,      slotX + 18, slotY + 18, strokeColor);
    }
}
