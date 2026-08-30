package com.ogatamizuki.guide.client.page;

import com.ogatamizuki.guide.model.GuidePage;
import com.ogatamizuki.guide.model.GuideTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.List;

/** タブレット向け: エントリ内の全ページを縦に連結してスクロール表示する。 */
public final class GuideEntryScrollContent {
    private static final int PAGE_GAP = 12;

    private GuideEntryScrollContent() {}

    public static int totalHeight(Font font, GuideTheme theme, List<GuidePage> pages, int width, int viewportHeight) {
        if (pages.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < pages.size(); i++) {
            total += pageHeight(font, theme, pages.get(i), width, viewportHeight);
            if (i < pages.size() - 1) {
                total += PAGE_GAP;
            }
        }
        return total;
    }

    public static void render(
            GuiGraphicsExtractor gui,
            Font font,
            GuideTheme theme,
            List<GuidePage> pages,
            int x,
            int y,
            int width,
            int viewportHeight,
            int clipTop,
            int clipBottom,
            int scrollOffset,
            int mouseX,
            int mouseY
    ) {
        int lineY = y - scrollOffset;
        for (int i = 0; i < pages.size(); i++) {
            GuidePage page = pages.get(i);
            PageRenderer renderer = PageRenderers.get(page.type());
            int pageHeight = pageHeight(font, theme, page, width, viewportHeight);
            if (lineY + pageHeight > clipTop && lineY < clipBottom) {
                renderer.render(
                        gui,
                        theme,
                        page,
                        x,
                        lineY,
                        width,
                        pageHeight,
                        mouseX,
                        mouseY,
                        0
                );
            }
            lineY += pageHeight;
            if (i < pages.size() - 1) {
                lineY += PAGE_GAP;
            }
        }
    }

    public static boolean mouseClicked(
            Font font,
            GuideTheme theme,
            List<GuidePage> pages,
            int x,
            int y,
            int width,
            int viewportHeight,
            int scrollOffset,
            MouseButtonEvent event
    ) {
        double mouseY = event.y();
        int lineY = y - scrollOffset;
        for (int i = 0; i < pages.size(); i++) {
            GuidePage page = pages.get(i);
            int pageHeight = pageHeight(font, theme, page, width, viewportHeight);
            if (mouseY >= lineY && mouseY < lineY + pageHeight) {
                return PageRenderers.get(page.type()).mouseClicked(
                        null,
                        theme,
                        page,
                        x,
                        lineY,
                        width,
                        pageHeight,
                        event
                );
            }
            lineY += pageHeight;
            if (i < pages.size() - 1) {
                lineY += PAGE_GAP;
            }
        }
        return false;
    }

    private static int pageHeight(Font font, GuideTheme theme, GuidePage page, int width, int viewportHeight) {
        return PageRenderers.get(page.type()).contentHeight(theme, page, width, viewportHeight);
    }
}
