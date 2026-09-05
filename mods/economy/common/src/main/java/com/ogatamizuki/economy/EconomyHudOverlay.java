package com.ogatamizuki.economy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;

import java.text.NumberFormat;
import java.util.Locale;

public class EconomyHudOverlay {
    private static final NumberFormat YEN_FORMAT = NumberFormat.getNumberInstance(Locale.JAPAN);
    private static boolean loggedRender = false;

    public static void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden()) {
            return;
        }
        if (!EconomyFeatures.isBalanceHudEnabled()) {
            return;
        }
        if (!EconomyCommon.isEconomyReady()) {
            return;
        }

        if (!loggedRender) {
            EconomyCommon.LOGGER.info("EconomyHudOverlay.render() called successfully at least once! Current balance: {}, Bank balance: {}", 
                    EconomyCommon.getCurrentBalance(), EconomyCommon.getCurrentBankBalance());
            loggedRender = true;
        }

        Font font = mc.font;
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        String balanceText = EconomyMasterI18n.tr(
                "economy.hud.balance",
                EconomyMasterI18n.formatCurrency(EconomyCommon.getCurrentBalance()),
                EconomyMasterI18n.formatCurrency(EconomyCommon.getCurrentBankBalance())
        ).getString();
        int textWidth = font.width(balanceText);

        // 画面下部中央、ホットバーや経験値・体力ゲージより少し上に配置
        int x = (width - textWidth) / 2;
        int y = height - 60; 

        // テキストの描画（影付きで経済をイメージした金色: 0xFFFFD700）
        guiGraphics.text(font, balanceText, x, y, 0xFFFFD700, true);
    }
}

