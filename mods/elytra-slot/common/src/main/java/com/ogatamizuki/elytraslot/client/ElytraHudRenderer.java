package com.ogatamizuki.elytraslot.client;

import com.ogatamizuki.elytraslot.Config;
import com.ogatamizuki.elytraslot.ElytraSlotCommon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ElytraHudRenderer {

    public static void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        if (!Config.HUD_ENABLED.get()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.gui.hud.isHidden()) {
            return;
        }

        ItemStack elytra = ElytraSlotCommon.getElytra(player);
        ItemStack firework = ElytraSlotCommon.getFirework(player);

        if (elytra.isEmpty() && firework.isEmpty()) {
            return;
        }

        Font font = mc.font;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // 1. Render Elytra HUD if slot has elytra
        if (!elytra.isEmpty()) {
            int ex = (screenWidth / 2) + Config.ELYTRA_HUD_X.get();
            int ey = screenHeight + Config.ELYTRA_HUD_Y.get();

            guiGraphics.fakeItem(elytra, ex, ey);

            int maxDamage = elytra.getMaxDamage();
            int damage = elytra.getDamageValue();
            int remaining = maxDamage - damage;
            double pct = maxDamage > 0 ? (double) remaining / maxDamage : 0.0;

            String pctText = String.format("%d%%", (int) (pct * 100));
            int color = pct <= Config.WARNING_THRESHOLD.get() ? 0xFFFF5555 : 0xFFFFFFFF; // Red if low, else white

            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().scale(0.75f, 0.75f);
            guiGraphics.text(font, pctText, (int) ((ex + 14) / 0.75f), (int) ((ey + 10) / 0.75f), color, true);
            guiGraphics.pose().popMatrix();
        }

        // 2. Render Firework HUD if slot has fireworks
        if (!firework.isEmpty()) {
            int fx = (screenWidth / 2) + Config.FIREWORK_HUD_X.get();
            int fy = screenHeight + Config.FIREWORK_HUD_Y.get();

            guiGraphics.fakeItem(firework, fx, fy);

            String countText = String.valueOf(firework.getCount());
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().scale(0.85f, 0.85f);
            guiGraphics.text(font, countText, (int) ((fx + 12) / 0.85f), (int) ((fy + 10) / 0.85f), 0xFFFFFFFF, true);
            guiGraphics.pose().popMatrix();
        }
    }
}
