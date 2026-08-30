package com.ogatamizuki.guide.client.screen;

import com.ogatamizuki.guide.model.GuideTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Patchouli 風のテキストリンク一覧。ボタンウィジェットを使わず描画＋クリック判定する。
 */
public final class GuideLinkList {
    private static final int LINE_GAP = 5;
    private static final int ICON_TEXT_GAP = 4;

    /** リンクの描画先。羊皮紙内と暗い外枠で色を分ける。 */
    public enum LinkSurface {
        CONTENT,
        PANEL
    }

    private record HitArea(int x, int y, int width, int height, Runnable action) {}

    private final List<HitArea> hitAreas = new ArrayList<>();

    public void clear() {
        hitAreas.clear();
    }

    public static int listHeight(Font font, int entryCount) {
        return listHeight(font, entryCount, false);
    }

    public static int listHeight(Font font, int entryCount, boolean hasIcons) {
        if (entryCount <= 0) {
            return 0;
        }
        if (hasIcons) {
            return GuideUi.listAreaHeight(entryCount);
        }
        return entryCount * (font.lineHeight + LINE_GAP) - LINE_GAP;
    }

    public static int listHeight(Font font, List<LinkItem> items) {
        return listHeight(font, items, 1);
    }

    public static int listHeight(Font font, List<LinkItem> items, int columns) {
        if (items.isEmpty()) {
            return 0;
        }
        columns = Math.max(1, columns);
        if (columns == 1) {
            return listHeightSingleColumn(font, items);
        }
        return listHeightMultiColumn(font, items, columns);
    }

    private static int listHeightSingleColumn(Font font, List<LinkItem> items) {
        int total = 0;
        for (int i = 0; i < items.size(); i++) {
            total += itemRowHeight(font, items.get(i));
            if (i < items.size() - 1) {
                total += itemRowGap(items.get(i));
            }
        }
        return total;
    }

    private static int listHeightMultiColumn(Font font, List<LinkItem> items, int columns) {
        return layoutHeight(buildRowLayout(font, items, columns));
    }

    private record ListRowLayout(int height, int gapAfter, List<Integer> itemIndices) {}

    private static List<ListRowLayout> buildRowLayout(Font font, List<LinkItem> items, int columns) {
        List<ListRowLayout> rows = new ArrayList<>();
        List<Integer> rowItemIndices = new ArrayList<>();
        int rowHeight = 0;
        int col = 0;
        LinkItem previous = null;

        for (int i = 0; i < items.size(); i++) {
            LinkItem item = items.get(i);
            if (item.isHeading()) {
                if (!rowItemIndices.isEmpty()) {
                    rows.add(new ListRowLayout(rowHeight, rowGap(previous), List.copyOf(rowItemIndices)));
                    rowItemIndices = new ArrayList<>();
                    col = 0;
                    rowHeight = 0;
                } else if (!rows.isEmpty() && previous != null) {
                    ListRowLayout last = rows.get(rows.size() - 1);
                    rows.set(rows.size() - 1, new ListRowLayout(last.height, rowGap(previous), last.itemIndices));
                }
                int gapAfter = i + 1 < items.size() ? rowGap(item) : 0;
                rows.add(new ListRowLayout(itemRowHeight(font, item), gapAfter, List.of(i)));
                col = 0;
                previous = item;
                continue;
            }

            if (col == 0) {
                rowItemIndices = new ArrayList<>();
                rowHeight = itemRowHeight(font, item);
            }
            rowItemIndices.add(i);
            col++;
            previous = item;

            if (col >= columns) {
                rows.add(new ListRowLayout(rowHeight, rowGap(item), List.copyOf(rowItemIndices)));
                rowItemIndices = new ArrayList<>();
                col = 0;
                rowHeight = 0;
            }
        }

        if (!rowItemIndices.isEmpty()) {
            rows.add(new ListRowLayout(rowHeight, 0, List.copyOf(rowItemIndices)));
        }
        return rows;
    }

    private static int layoutHeight(List<ListRowLayout> rows) {
        int total = 0;
        for (ListRowLayout row : rows) {
            total += row.height + row.gapAfter;
        }
        return total;
    }

    public static int itemRowHeight(Font font, LinkItem item) {
        return rowHeight(font, item);
    }

    public static int itemRowGap(LinkItem item) {
        return rowGap(item);
    }

    private static int rowHeight(Font font, LinkItem item) {
        if (item.isHeading()) {
            return font.lineHeight + 2;
        }
        return item.hasIcon() ? GuideUi.ROW_HEIGHT : font.lineHeight;
    }

    private static int rowGap(LinkItem item) {
        return item.hasIcon() ? GuideUi.ROW_GAP : LINE_GAP;
    }

    public int render(
            GuiGraphicsExtractor gui,
            Font font,
            GuideTheme theme,
            int x,
            int y,
            int width,
            List<LinkItem> items,
            int mouseX,
            int mouseY
    ) {
        return render(gui, font, theme, x, y, width, items, mouseX, mouseY, LinkSurface.CONTENT, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
    }

    public int render(
            GuiGraphicsExtractor gui,
            Font font,
            GuideTheme theme,
            int x,
            int y,
            int width,
            List<LinkItem> items,
            int mouseX,
            int mouseY,
            LinkSurface surface
    ) {
        return render(gui, font, theme, x, y, width, items, mouseX, mouseY, surface, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
    }

    public int render(
            GuiGraphicsExtractor gui,
            Font font,
            GuideTheme theme,
            int x,
            int y,
            int width,
            List<LinkItem> items,
            int mouseX,
            int mouseY,
            LinkSurface surface,
            int scrollOffset,
            int clipTop,
            int clipBottom
    ) {
        return render(gui, font, theme, x, y, width, items, mouseX, mouseY, surface, scrollOffset, clipTop, clipBottom, 1);
    }

    public int render(
            GuiGraphicsExtractor gui,
            Font font,
            GuideTheme theme,
            int x,
            int y,
            int width,
            List<LinkItem> items,
            int mouseX,
            int mouseY,
            LinkSurface surface,
            int scrollOffset,
            int clipTop,
            int clipBottom,
            int columns
    ) {
        columns = Math.max(1, columns);
        if (columns == 1) {
            return renderSingleColumn(gui, font, theme, x, y, width, items, mouseX, mouseY, surface, scrollOffset, clipTop, clipBottom);
        }
        return renderMultiColumn(gui, font, theme, x, y, width, items, mouseX, mouseY, surface, scrollOffset, clipTop, clipBottom, columns);
    }

    private int renderSingleColumn(
            GuiGraphicsExtractor gui,
            Font font,
            GuideTheme theme,
            int x,
            int y,
            int width,
            List<LinkItem> items,
            int mouseX,
            int mouseY,
            LinkSurface surface,
            int scrollOffset,
            int clipTop,
            int clipBottom
    ) {
        int lineY = y - scrollOffset;
        for (LinkItem item : items) {
            int rowHeight = rowHeight(font, item);
            int rowGap = rowGap(item);
            if (rowVisible(lineY, rowHeight, clipTop, clipBottom)) {
                renderLine(gui, font, theme, x, lineY, width, item, mouseX, mouseY, false, surface);
            }
            lineY += rowHeight + rowGap;
        }
        return lineY + scrollOffset;
    }

    private int renderMultiColumn(
            GuiGraphicsExtractor gui,
            Font font,
            GuideTheme theme,
            int x,
            int y,
            int width,
            List<LinkItem> items,
            int mouseX,
            int mouseY,
            LinkSurface surface,
            int scrollOffset,
            int clipTop,
            int clipBottom,
            int columns
    ) {
        int columnWidth = columnWidth(width, columns);
        int lineY = y - scrollOffset;
        for (ListRowLayout row : buildRowLayout(font, items, columns)) {
            if (rowVisible(lineY, row.height, clipTop, clipBottom)) {
                if (row.itemIndices.size() == 1 && items.get(row.itemIndices.get(0)).isHeading()) {
                    renderLine(gui, font, theme, x, lineY, width, items.get(row.itemIndices.get(0)), mouseX, mouseY, false, surface);
                } else {
                    int col = 0;
                    for (int itemIndex : row.itemIndices) {
                        int colX = x + col * (columnWidth + GuideUi.LIST_COLUMN_GAP);
                        renderLine(gui, font, theme, colX, lineY, columnWidth, items.get(itemIndex), mouseX, mouseY, false, surface);
                        col++;
                    }
                }
            }
            lineY += row.height + row.gapAfter;
        }
        return lineY + scrollOffset;
    }

    private static int columnWidth(int totalWidth, int columns) {
        if (columns <= 1) {
            return totalWidth;
        }
        return (totalWidth - GuideUi.LIST_COLUMN_GAP * (columns - 1)) / columns;
    }

    private static boolean rowVisible(int lineY, int rowHeight, int clipTop, int clipBottom) {
        return lineY + rowHeight > clipTop && lineY < clipBottom;
    }

    public int renderCentered(
            GuiGraphicsExtractor gui,
            Font font,
            GuideTheme theme,
            int centerX,
            int y,
            Component label,
            Runnable action,
            int mouseX,
            int mouseY
    ) {
        return renderCentered(gui, font, theme, centerX, y, label, action, mouseX, mouseY, LinkSurface.CONTENT);
    }

    public int renderCentered(
            GuiGraphicsExtractor gui,
            Font font,
            GuideTheme theme,
            int centerX,
            int y,
            Component label,
            Runnable action,
            int mouseX,
            int mouseY,
            LinkSurface surface
    ) {
        return renderLine(gui, font, theme, centerX, y, Integer.MAX_VALUE, LinkItem.plain(label, action), mouseX, mouseY, true, surface);
    }

    private static int linkColor(GuideTheme theme, LinkSurface surface, boolean hovered) {
        if (surface == LinkSurface.PANEL) {
            return hovered ? theme.colorAccent() : theme.colorTitle();
        }
        return hovered ? theme.colorLinkHover() : theme.colorLink();
    }

    private int renderLine(
            GuiGraphicsExtractor gui,
            Font font,
            GuideTheme theme,
            int x,
            int y,
            int width,
            LinkItem item,
            int mouseX,
            int mouseY,
            boolean center,
            LinkSurface surface
    ) {
        if (item.isHeading()) {
            gui.text(font, item.label().getVisualOrderText(), x, y + 1, theme.colorSubtitle(), false);
            return y + rowHeight(font, item) + rowGap(item);
        }

        int rowHeight = rowHeight(font, item);
        int textY = item.hasIcon() ? y + (GuideUi.ROW_HEIGHT - font.lineHeight) / 2 : y;
        int iconLeading = item.hasIcon() && surface == LinkSurface.CONTENT ? GuideUi.LIST_ICON_LEADING : 0;
        int contentX = x + iconLeading;

        if (item.hasIcon()) {
            int iconY = y + (GuideUi.ROW_HEIGHT - 18) / 2;
            GuideUi.drawItemStack(theme, gui, item.icon(), contentX, iconY);
            GuideItemTooltips.renderItemTooltip(gui, font, item.icon(), mouseX, mouseY, contentX, iconY, 18);
            contentX = contentX + 18 + ICON_TEXT_GAP;
        }

        Component plain = item.bullet() && !item.hasIcon()
                ? Component.literal("• ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(theme.colorMuted())))
                .append(item.label())
                : item.label();
        FormattedCharSequence plainVisual = plain.getVisualOrderText();
        int textWidth = font.width(plainVisual);
        int drawX = center ? x - textWidth / 2 : contentX;

        boolean hovered = mouseX >= drawX && mouseX < drawX + textWidth && mouseY >= textY && mouseY < textY + font.lineHeight;
        int linkColor = linkColor(theme, surface, hovered);

        Component text = item.bullet() && !item.hasIcon()
                ? Component.literal("• ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(theme.colorMuted())))
                .append(styledLink(item.label(), linkColor))
                : styledLink(item.label(), linkColor);

        FormattedCharSequence visual = text.getVisualOrderText();
        gui.text(font, visual, drawX, textY, 0xFFFFFFFF, false);

        int hitX = item.hasIcon() ? x + iconLeading : drawX;
        int hitWidth = item.hasIcon() ? Math.min(width, contentX - x + textWidth) : textWidth;
        hitAreas.add(new HitArea(hitX, y, hitWidth, rowHeight, item.action()));
        return y + rowHeight + rowGap(item);
    }

    private static Component styledLink(Component label, int color) {
        return label.copy().withStyle(style -> style
                .withUnderlined(true)
                .withColor(TextColor.fromRgb(color)));
    }

    public boolean mouseClicked(MouseButtonEvent event) {
        if (event.button() != 0) {
            return false;
        }
        double mouseX = event.x();
        double mouseY = event.y();
        for (HitArea area : hitAreas) {
            if (mouseX >= area.x && mouseX < area.x + area.width
                    && mouseY >= area.y && mouseY < area.y + area.height) {
                area.action().run();
                return true;
            }
        }
        return false;
    }

    public record LinkItem(Component label, @Nullable Runnable action, boolean bullet, ItemStack icon) {
        public static LinkItem of(Component label, Runnable action) {
            return new LinkItem(label, action, true, ItemStack.EMPTY);
        }

        public static LinkItem withIcon(Component label, ItemStack icon, Runnable action) {
            ItemStack displayIcon = icon == null ? ItemStack.EMPTY : icon.copy();
            return new LinkItem(label, action, false, displayIcon);
        }

        public static LinkItem plain(Component label, Runnable action) {
            return new LinkItem(label, action, false, ItemStack.EMPTY);
        }

        public static LinkItem heading(Component label) {
            return new LinkItem(label, null, false, ItemStack.EMPTY);
        }

        public boolean hasIcon() {
            return !icon.isEmpty();
        }

        public boolean isHeading() {
            return action == null;
        }
    }
}
