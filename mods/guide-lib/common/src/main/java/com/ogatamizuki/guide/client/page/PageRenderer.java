package com.ogatamizuki.guide.client.page;

import com.ogatamizuki.guide.model.GuidePage;
import com.ogatamizuki.guide.model.GuideTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public interface PageRenderer {
    void render(
            GuiGraphicsExtractor gui,
            GuideTheme theme,
            GuidePage page,
            int x,
            int y,
            int width,
            int height,
            int mouseX,
            int mouseY
    );

    default void render(
            GuiGraphicsExtractor gui,
            GuideTheme theme,
            GuidePage page,
            int x,
            int y,
            int width,
            int height,
            int mouseX,
            int mouseY,
            int scrollOffset
    ) {
        render(gui, theme, page, x, y, width, height, mouseX, mouseY);
    }

    /** ページ本文の描画高さ（スクロール判定用） */
    default int contentHeight(GuideTheme theme, GuidePage page, int width, int height) {
        return height;
    }

    default boolean mouseClicked(
            GuiGraphicsExtractor gui,
            GuideTheme theme,
            GuidePage page,
            int x,
            int y,
            int width,
            int height,
            MouseButtonEvent event
    ) {
        return false;
    }
}
