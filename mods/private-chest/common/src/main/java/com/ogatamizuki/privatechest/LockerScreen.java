package com.ogatamizuki.privatechest;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class LockerScreen extends AbstractContainerScreen<LockerMenu> {

    // ウィンドウサイズ（スクロールバー12px分を右に追加）
    private static final int WIN_WIDTH  = 190;
    // 高さ = タイトル(18) + ロッカー5行×18(90) + 区切り(14) + プレイヤー3行×18(54) + 区切り(4) + ホットバー(18) + 下余白(7)
    private static final int WIN_HEIGHT = 18 + 90 + 14 + 54 + 4 + 18 + 7; // = 205

    // スクロールバー（ウィンドウ相対座標）
    private static final int SCROLL_BAR_X  = 171;
    private static final int SCROLL_BAR_Y  = LockerMenu.LOCKER_SLOT_START_Y - 1;   // = 17
    private static final int SCROLL_BAR_W  = 12;
    private static final int SCROLL_BAR_H  = LockerMenu.VISIBLE_ROWS * 18;          // = 90
    private static final int SCROLL_KNOB_H = 18;

    private float scrollOffset = 0.0f;
    private int currentTopRow = 0;

    public LockerScreen(LockerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, WIN_WIDTH, WIN_HEIGHT);
        // LockerMenu の定数からプレイヤーインベントリラベルの Y を決定
        this.inventoryLabelY = LockerMenu.PLAYER_INV_Y - 10;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;

        // ---- 外枠 ----
        drawBevel(gui, x, y, x + this.imageWidth, y + this.imageHeight, false);

        // ---- ロッカースロットエリア（Menu定数に合わせた描画範囲） ----
        int lockerAreaTop    = y + LockerMenu.LOCKER_SLOT_START_Y - 1;                             // y+17
        int lockerAreaBottom = y + LockerMenu.LOCKER_SLOT_START_Y + LockerMenu.VISIBLE_ROWS * 18;  // y+108
        drawBevel(gui, x + 7, lockerAreaTop, x + 169, lockerAreaBottom, true);

        for (int r = 0; r < LockerMenu.VISIBLE_ROWS; r++) {
            for (int c = 0; c < LockerMenu.COLS; c++) {
                // スロット枠を1px左上にずらす（x+7, y-1）
                drawSlotBorder(gui, x + 7 + c * 18, y + LockerMenu.LOCKER_SLOT_START_Y + r * 18 - 1);
            }
        }

        // ---- スクロールバー背景 ----
        int sbX = x + SCROLL_BAR_X;
        int sbY = y + SCROLL_BAR_Y;
        drawBevel(gui, sbX, sbY, sbX + SCROLL_BAR_W, sbY + SCROLL_BAR_H, true);

        // ---- スクロールつまみ ----
        int maxScroll = SCROLL_BAR_H - SCROLL_KNOB_H;
        int knobY = sbY + (int)(scrollOffset * maxScroll);
        gui.fill(sbX + 2, knobY + 2,                    sbX + SCROLL_BAR_W - 2, knobY + SCROLL_KNOB_H - 2, 0xFFAAAAAA);
        gui.fill(sbX + 2, knobY + 2,                    sbX + SCROLL_BAR_W - 2, knobY + 3,                  0xFFCCCCCC);
        gui.fill(sbX + 2, knobY + 2,                    sbX + 3,                knobY + SCROLL_KNOB_H - 2, 0xFFCCCCCC);
        gui.fill(sbX + 2, knobY + SCROLL_KNOB_H - 3,    sbX + SCROLL_BAR_W - 2, knobY + SCROLL_KNOB_H - 2, 0xFF555555);
        gui.fill(sbX + SCROLL_BAR_W - 3, knobY + 2,     sbX + SCROLL_BAR_W - 2, knobY + SCROLL_KNOB_H - 2, 0xFF555555);

        // ---- プレイヤーインベントリ（Menu定数と完全一致） ----
        int playerAreaTop    = y + LockerMenu.PLAYER_INV_Y - 1;
        int playerAreaBottom = y + LockerMenu.PLAYER_INV_Y + 3 * 18;
        drawBevel(gui, x + 7, playerAreaTop, x + 169, playerAreaBottom, true);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                drawSlotBorder(gui, x + 7 + c * 18, y + LockerMenu.PLAYER_INV_Y + r * 18 - 1);
            }
        }

        // ---- ホットバー（Menu定数と完全一致） ----
        int hotbarAreaTop    = y + LockerMenu.HOTBAR_Y - 1;
        int hotbarAreaBottom = y + LockerMenu.HOTBAR_Y + 18;
        drawBevel(gui, x + 7, hotbarAreaTop, x + 169, hotbarAreaBottom, true);
        for (int c = 0; c < 9; c++) {
            drawSlotBorder(gui, x + 7 + c * 18, y + LockerMenu.HOTBAR_Y - 1);
        }

        super.extractContents(gui, mouseX, mouseY, partialTick);
    }

    // ---- マウスホイールスクロール ----
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxRow = LockerMenu.TOTAL_ROWS - LockerMenu.VISIBLE_ROWS;
        scrollOffset = Mth.clamp(scrollOffset - (float)(scrollY / maxRow), 0.0f, 1.0f);
        applyScroll();
        return true;
    }

    private void applyScroll() {
        int maxRow = LockerMenu.TOTAL_ROWS - LockerMenu.VISIBLE_ROWS;
        currentTopRow = Math.round(scrollOffset * maxRow);
        this.menu.scrollTo(currentTopRow);
    }

    // ---- 描画ユーティリティ ----
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
