package com.ogatamizuki.guide.client.page;

import com.ogatamizuki.guide.client.GuideLibClient;
import com.ogatamizuki.guide.client.text.GuideTextLayout;
import com.ogatamizuki.guide.client.jei.GuideJeiBridge;
import com.ogatamizuki.guide.client.screen.GuideItemTooltips;
import com.ogatamizuki.guide.client.screen.GuideUi;
import com.ogatamizuki.guide.model.GuidePage;
import com.ogatamizuki.guide.model.GuideTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class SpotlightPageRenderer implements PageRenderer {
    private int lastIconX;
    private int lastIconY;
    private ItemStack lastStack = ItemStack.EMPTY;

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
        Font font = Minecraft.getInstance().font;
        ItemStack stack = ItemStack.EMPTY;
        if (page.data().has("item")) {
            try {
                Identifier itemId = Identifier.parse(page.data().get("item").getAsString());
                var item = BuiltInRegistries.ITEM.getValue(itemId);
                if (item != null) {
                    stack = new ItemStack(item);
                }
            } catch (Exception ignored) {
            }
        }

        int iconX = x + (width - 18) / 2;
        int iconY = y + GuideUi.SPOTLIGHT_ICON_TOP;
        this.lastIconX = iconX;
        this.lastIconY = iconY;
        this.lastStack = stack;
        GuideUi.drawItemStack(theme, gui, stack, iconX, iconY);
        GuideItemTooltips.renderItemTooltip(gui, font, stack, mouseX, mouseY, iconX, iconY, 18);
        if (!stack.isEmpty()) {
            Component name = stack.getHoverName();
            int nameWidth = font.width(name);
            gui.text(font, name, x + (width - nameWidth) / 2, iconY + 24, theme.colorTitle(), false);
        }

        if (page.data().has("text")) {
            Component text = GuideLibClient.translateOrLiteral(page.data().get("text").getAsString());
            List<net.minecraft.util.FormattedCharSequence> lines = GuideTextLayout.split(font, text, width - 16);
            int textStartY = iconY + GuideUi.SPOTLIGHT_TEXT_OFFSET;
            int clipBottom = y + height - GuideUi.PAGE_TEXT_BOTTOM;
            int lineY = textStartY - scrollOffset;
            for (var line : lines) {
                if (lineY + font.lineHeight > textStartY - scrollOffset && lineY < clipBottom) {
                    gui.text(font, line, x + 8, lineY, theme.colorBody(), false);
                }
                lineY += font.lineHeight + 2;
            }
        }
    }

    @Override
    public int contentHeight(GuideTheme theme, GuidePage page, int width, int height) {
        if (!page.data().has("text")) {
            return GuideUi.SPOTLIGHT_HEADER_HEIGHT;
        }
        Font font = Minecraft.getInstance().font;
        Component text = GuideLibClient.translateOrLiteral(page.data().get("text").getAsString());
        int lineCount = GuideTextLayout.split(font, text, width - 16).size();
        return GuideUi.SPOTLIGHT_HEADER_HEIGHT
                + GuideUi.wrappedLinesHeight(font, lineCount)
                + GuideUi.PAGE_TEXT_BOTTOM;
    }

    @Override
    public boolean mouseClicked(
            GuiGraphicsExtractor gui,
            GuideTheme theme,
            GuidePage page,
            int x,
            int y,
            int width,
            int height,
            MouseButtonEvent event
    ) {
        if (lastStack.isEmpty()) {
            return false;
        }
        int button = event.button();
        if (button != 0 && button != 1) {
            return false;
        }
        if (!GuideItemTooltips.isMouseOver((int) event.x(), (int) event.y(), lastIconX, lastIconY, 18)) {
            return false;
        }
        return GuideJeiBridge.handleItemClick(lastStack, button);
    }
}
