package com.ogatamizuki.sleep;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.gui.GuiLayer;

public final class SleepHealthOverlay {
    private static final float SLEEP_HEALTH_SCALE = 1.5F;
    /** 1 段目ハート行の中心（extractHealthLevel の yLineBase からの概算オフセット） */
    private static final int HEART_ROW_CENTER_OFFSET = 4;
    /** 画面中央より少し上（「ベッドから出る」ボタンと重ならない位置） */
    private static final int TARGET_CENTER_Y_OFFSET = -12;
    /** extractHearts の 1 行目左端（Gui.extractHealthLevel と同値） */
    private static final int HEART_ROW_LEFT_OFFSET = 91;
    private static final int HEART_COLUMN_SPACING = 8;
    private static final int HEART_SPRITE_SIZE = 9;

    private SleepHealthOverlay() {
    }

    public static GuiLayer wrapPlayerHealth(GuiLayer original) {
        return (guiGraphics, deltaTracker) -> render(guiGraphics, deltaTracker, original);
    }

    private static void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, GuiLayer original) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }

        if (mc.player.isSleeping() && SleepClientFlags.healWhileSleeping()) {
            int width = guiGraphics.guiWidth();
            int height = guiGraphics.guiHeight();
            int defaultBaseY = height - mc.gui.leftHeight;
            float screenCenterX = width / 2f;
            float targetCenterY = height / 2f + TARGET_CENTER_Y_OFFSET;
            float heartRowCenterY = defaultBaseY + HEART_ROW_CENTER_OFFSET;
            float heartRowCenterX = heartRowCenterX(mc.player, width);

            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(screenCenterX, targetCenterY);
            guiGraphics.pose().scale(SLEEP_HEALTH_SCALE, SLEEP_HEALTH_SCALE);
            guiGraphics.pose().translate(-heartRowCenterX, -heartRowCenterY);
            original.render(guiGraphics, deltaTracker);
            guiGraphics.pose().popMatrix();
            return;
        }

        original.render(guiGraphics, deltaTracker);
    }

    /**
     * バニラ HUD は xLeft = width/2 - 91 から描画するが、1 行目の実幅は列数に依存するため
     * 見た目の中心は画面中央と一致しない。extractHearts と同じ列数・幅で中心 X を求める。
     */
    private static float heartRowCenterX(Player player, int screenWidth) {
        float maxHealth = Math.max(player.getMaxHealth(), player.getHealth());
        int healthContainerCount = Mth.ceil(maxHealth / 2.0F);
        int absorptionContainerCount = Mth.ceil(player.getAbsorptionAmount() / 2.0F);
        int columnsInFirstRow = Math.min(10, healthContainerCount + absorptionContainerCount);
        float rowWidth = (columnsInFirstRow - 1) * HEART_COLUMN_SPACING + HEART_SPRITE_SIZE;
        float xLeft = screenWidth / 2f - HEART_ROW_LEFT_OFFSET;
        return xLeft + rowWidth / 2f;
    }
}
