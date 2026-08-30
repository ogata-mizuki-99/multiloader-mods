package com.ogatamizuki.guide.client.page;

import com.ogatamizuki.guide.client.GuideLibClient;
import com.ogatamizuki.guide.client.screen.GuideUi;
import com.ogatamizuki.guide.client.text.GuideTextLayout;
import com.ogatamizuki.guide.model.GuidePage;
import com.ogatamizuki.guide.model.GuideTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

public class TextPageRenderer implements PageRenderer {
    private static final int TEXT_PADDING = 16;
    private static final int TEXT_TOP = 8;
    private static final int TEXT_BOTTOM = 12;

    @Override
    public void render(GuiGraphicsExtractor gui, GuideTheme theme, GuidePage page, int x, int y, int width, int height, int mouseX, int mouseY) {
        render(gui, theme, page, x, y, width, height, mouseX, mouseY, 0);
    }

    @Override
    public void render(
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
        if (!page.data().has("text")) {
            return;
        }
        Component text = GuideLibClient.translateOrLiteral(page.data().get("text").getAsString());
        Font font = Minecraft.getInstance().font;
        List<net.minecraft.util.FormattedCharSequence> lines = GuideTextLayout.split(font, text, width - TEXT_PADDING);
        int clipTop = y + TEXT_TOP;
        int clipBottom = y + height - TEXT_BOTTOM;
        int lineY = clipTop - scrollOffset;
        for (var line : lines) {
            if (lineY >= clipBottom) {
                break;
            }
            if (lineY + font.lineHeight > clipTop) {
                gui.text(font, line, x + 8, lineY, theme.colorBody(), false);
            }
            lineY += font.lineHeight + 2;
        }
    }

    @Override
    public int contentHeight(GuideTheme theme, GuidePage page, int width, int height) {
        if (!page.data().has("text")) {
            return 0;
        }
        Component text = GuideLibClient.translateOrLiteral(page.data().get("text").getAsString());
        Font font = Minecraft.getInstance().font;
        int lineCount = GuideTextLayout.split(font, text, width - TEXT_PADDING).size();
        return TEXT_TOP + GuideUi.wrappedLinesHeight(font, lineCount) + TEXT_BOTTOM;
    }
}
