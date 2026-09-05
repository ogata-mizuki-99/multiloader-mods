package com.ogatamizuki.deconstructor;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class DeconstructorScreen extends AbstractContainerScreen<DeconstructorMenu> {
    private static final int WIN_WIDTH = 176;
    private static final int WIN_HEIGHT = 202;
    /** Left/right padding inside the panel for the title string. */
    private static final int TITLE_PAD = 8;

    public DeconstructorScreen(DeconstructorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, WIN_WIDTH, WIN_HEIGHT);
        this.inventoryLabelY = 108;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gui, int mouseX, int mouseY) {
        int maxTitleWidth = this.imageWidth - TITLE_PAD * 2;
        Component title = ellipsize(this.title, maxTitleWidth);
        gui.text(this.font, title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
        gui.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
    }

    private Component ellipsize(Component text, int maxWidth) {
        String s = text.getString();
        if (this.font.width(s) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisW = this.font.width(ellipsis);
        int end = s.length();
        while (end > 0 && this.font.width(s.substring(0, end)) + ellipsisW > maxWidth) {
            end--;
        }
        return Component.literal(s.substring(0, Math.max(0, end)) + ellipsis);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;

        // ---- 外枠 ----
        drawBevel(gui, x, y, x + this.imageWidth, y + this.imageHeight, false);

        // ---- 入力スロットエリア ----
        drawBevel(gui, x + 79, y + 19, x + 97, y + 37, true);
        drawSlotBorder(gui, x + 79, y + 19);

        // ---- プレビュースロットエリア (3x3) ----
        drawBevel(gui, x + 61, y + 55, x + 115, y + 109, true);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                drawSlotBorder(gui, x + 61 + c * 18, y + 55 + r * 18);
            }
        }

        // ---- プレイヤーインベントリ ----
        drawBevel(gui, x + 7, y + 119, x + 169, y + 173, true);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                drawSlotBorder(gui, x + 7 + c * 18, y + 119 + r * 18);
            }
        }

        // ---- ホットバー ----
        drawBevel(gui, x + 7, y + 177, x + 169, y + 195, true);
        for (int c = 0; c < 9; c++) {
            drawSlotBorder(gui, x + 7 + c * 18, y + 177);
        }

        super.extractContents(gui, mouseX, mouseY, partialTick);

        // 梯子など成果物が複数個のレシピ: 解体に必要な個数を入力スロット右に表示
        int required = this.menu.getRecipeOutputCount();
        if (required > 1) {
            ItemStack input = this.menu.getSlot(0).getItem();
            boolean enough = !input.isEmpty() && input.getCount() >= required;
            int color = enough ? 0xFF55FF55 : 0xFFFF5555;
            Component label = Component.translatable("gui.deconstructor.required_count", required);
            gui.text(this.font, label, x + 100, y + 24, color, false);
        }
    }

    // 描画ユーティリティ
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
