package com.ogatamizuki.guide.client.page;

import com.ogatamizuki.guide.client.GuideLibClient;
import com.ogatamizuki.guide.client.screen.GuideUi;
import com.ogatamizuki.guide.client.text.GuideTextLayout;
import com.ogatamizuki.guide.model.GuidePage;
import com.ogatamizuki.guide.model.GuideTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public class ImagePageRenderer implements PageRenderer {
    private static final int PADDING = 8;
    private static final int TEXT_TOP = 8;
    private static final int DEFAULT_TEX_SIZE = 128;
    private static final int MAX_IMAGE_HEIGHT = 160;

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
        Identifier texture = parseTexture(page);
        if (texture == null) {
            return;
        }

        int texW = page.data().has("width") ? page.data().get("width").getAsInt() : DEFAULT_TEX_SIZE;
        int texH = page.data().has("height") ? page.data().get("height").getAsInt() : DEFAULT_TEX_SIZE;
        int maxW = width - PADDING * 2;
        int displayW = Math.min(maxW, texW);
        int displayH = Math.max(1, (int) (displayW * (texH / (float) texW)));
        displayH = Math.min(displayH, MAX_IMAGE_HEIGHT);

        int imageX = x + (width - displayW) / 2;
        int imageY = y + PADDING - scrollOffset;
        gui.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                imageX,
                imageY,
                0.0F,
                0.0F,
                displayW,
                displayH,
                texW,
                texH
        );

        if (!page.data().has("text")) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        Component text = GuideLibClient.translateOrLiteral(page.data().get("text").getAsString());
        List<net.minecraft.util.FormattedCharSequence> lines = GuideTextLayout.split(font, text, width - PADDING);
        int textStartY = imageY + displayH + TEXT_TOP;
        int clipBottom = y + height - GuideUi.PAGE_TEXT_BOTTOM;
        int lineY = textStartY;
        for (var line : lines) {
            if (lineY >= clipBottom) {
                break;
            }
            if (lineY + font.lineHeight > y + GuideUi.PAGE_TEXT_TOP - scrollOffset) {
                gui.text(font, line, x + 8, lineY, theme.colorBody(), false);
            }
            lineY += font.lineHeight + 2;
        }
    }

    @Override
    public int contentHeight(GuideTheme theme, GuidePage page, int width, int height) {
        Identifier texture = parseTexture(page);
        if (texture == null) {
            return 0;
        }

        int texW = page.data().has("width") ? page.data().get("width").getAsInt() : DEFAULT_TEX_SIZE;
        int texH = page.data().has("height") ? page.data().get("height").getAsInt() : DEFAULT_TEX_SIZE;
        int maxW = width - PADDING * 2;
        int displayW = Math.min(maxW, texW);
        int displayH = Math.min(Math.max(1, (int) (displayW * (texH / (float) texW))), MAX_IMAGE_HEIGHT);
        int total = PADDING + displayH + TEXT_TOP;

        if (page.data().has("text")) {
            Font font = Minecraft.getInstance().font;
            Component text = GuideLibClient.translateOrLiteral(page.data().get("text").getAsString());
            int lineCount = GuideTextLayout.split(font, text, width - PADDING).size();
            total += GuideUi.wrappedLinesHeight(font, lineCount) + GuideUi.PAGE_TEXT_BOTTOM;
        } else {
            total += GuideUi.PAGE_TEXT_BOTTOM;
        }
        return total;
    }

    private static Identifier parseTexture(GuidePage page) {
        if (!page.data().has("image")) {
            return null;
        }
        try {
            return Identifier.parse(page.data().get("image").getAsString());
        } catch (Exception ignored) {
            return null;
        }
    }
}
