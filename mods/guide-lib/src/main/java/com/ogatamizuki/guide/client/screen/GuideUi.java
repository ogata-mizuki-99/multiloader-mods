package com.ogatamizuki.guide.client.screen;

import com.ogatamizuki.guide.model.GuideTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class GuideUi {
    public static final int PANEL_WIDTH = 276;
    public static final int HEADER_HEIGHT = 22;
    public static final int FOOTER_HEIGHT = 28;
    public static final int ROW_HEIGHT = 20;
    public static final int ROW_GAP = 4;
    public static final int SIDE_PADDING = 12;
    public static final int MAX_VISIBLE_LIST_ROWS = 12;
    /** 本テーマ目次: 1ページあたりの項目数 */
    public static final int BOOK_ITEMS_PER_PAGE = 5;
    /** タブレット目次: スクロール表示領域の行数 */
    public static final int TABLET_VISIBLE_LIST_ROWS = 5;
    public static final int SCROLL_STEP = 20;
    public static final int LIST_CONTENT_PADDING = 8;
    /** タブレット内枠線からスクロール開始までの余白 */
    public static final int INNER_PANEL_INSET = 4;
    public static final int TABLET_SCROLL_TOP_PADDING = 6;
    /** 目次リスト下端の余白（描画クリップ・ページ分割に使用） */
    public static final int LIST_BOTTOM_INSET = 16;
    /** 羊皮紙内コンテンツ上端余白 */
    public static final int PAPER_TOP_PADDING = 8;
    /** 画面上下端とパネル外枠の最小余白 */
    public static final int SCREEN_VERTICAL_MARGIN = 28;
    /** エントリ画面のパネル高さ（本テーマ） */
    public static final int ENTRY_PANEL_HEIGHT = 228;
    /** エントリ画面のパネル高さ（タブレット） */
    public static final int TABLET_ENTRY_PANEL_HEIGHT = 240;
    /** 本テーマ表紙: drawPaperCover の金線内側余白 */
    public static final int PAPER_COVER_INSET = 2;
    /** 本テーマ羊皮紙: 外枠から紙端まで */
    public static final int PAPER_SIDE_INSET = PAPER_COVER_INSET + 2;
    /** 本テーマ羊皮紙: 罫線開始位置（drawPaperPage と一致） */
    public static final int PAPER_RULED_INSET = 6;
    /** 本テーマ羊皮紙: 紙端からコンテンツまで */
    public static final int PAPER_CONTENT_INSET = PAPER_RULED_INSET + 10;
    /** 本テーマ羊皮紙: 下端クリップ余白 */
    public static final int PAPER_BOTTOM_CLIP_INSET = 2;
    /** 目次アイコン行: 左端の追加余白 */
    public static final int LIST_ICON_LEADING = 2;
    /** 目次リストの列間余白 */
    public static final int LIST_COLUMN_GAP = 8;
    /** タブレット目次: スクロール末尾の余白（最終行がクリップされないよう確保） */
    public static final int SCROLL_LIST_BOTTOM_PADDING = 4;
    /** 本テーマ目次: 羊皮紙下端の追加余白（アイコン行のはみ出し防止） */
    public static final int BOOK_TOC_BOTTOM_EXTRA = 32;
    /** スポットライトページ: アイコン上端オフセット */
    public static final int SPOTLIGHT_ICON_TOP = 10;
    /** スポットライトページ: アイコン上端から本文開始まで */
    public static final int SPOTLIGHT_TEXT_OFFSET = 40;
    /** スポットライトページ: ページ上端から本文開始まで（Expander と Renderer で共有） */
    public static final int SPOTLIGHT_HEADER_HEIGHT = SPOTLIGHT_ICON_TOP + SPOTLIGHT_TEXT_OFFSET;
    /** ページ本文: 上端・下端余白 */
    public static final int PAGE_TEXT_TOP = 8;
    public static final int PAGE_TEXT_BOTTOM = 12;

    /** @deprecated use {@link GuideTheme#colorTitle()} via theme instance */
    @Deprecated
    public static final int COLOR_TITLE = GuideTheme.bookBuiltin().colorTitle();
    /** @deprecated use theme instance */
    @Deprecated
    public static final int COLOR_SUBTITLE = GuideTheme.bookBuiltin().colorSubtitle();
    /** @deprecated use theme instance */
    @Deprecated
    public static final int COLOR_BODY = GuideTheme.bookBuiltin().colorBody();
    /** @deprecated use theme instance */
    @Deprecated
    public static final int COLOR_MUTED = GuideTheme.bookBuiltin().colorMuted();
    /** @deprecated use theme instance */
    @Deprecated
    public static final int COLOR_ERROR = GuideTheme.bookBuiltin().colorError();
    /** @deprecated use theme instance */
    @Deprecated
    public static final int COLOR_ACCENT = GuideTheme.bookBuiltin().colorAccent();

    private GuideUi() {}

    public static boolean isRightClick(MouseButtonEvent event) {
        return event.button() == 1;
    }

    public static boolean isPrevPageKey(KeyEvent event) {
        return event.key() == GLFW.GLFW_KEY_LEFT;
    }

    public static boolean isNextPageKey(KeyEvent event) {
        return event.key() == GLFW.GLFW_KEY_RIGHT;
    }

    public static boolean isMouseOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public static int paperLeft(int panelLeft) {
        return panelLeft + PAPER_SIDE_INSET;
    }

    public static int paperRight(int panelRight) {
        return panelRight - PAPER_SIDE_INSET;
    }

    public static int paperContentX(int panelLeft) {
        return paperLeft(panelLeft) + PAPER_CONTENT_INSET;
    }

    public static int paperContentWidth() {
        return PANEL_WIDTH - PAPER_SIDE_INSET * 2 - PAPER_CONTENT_INSET * 2;
    }

    public static int innerPanelLeft(int panelLeft, GuideTheme theme) {
        return theme.usesListScroll() ? panelLeft + SIDE_PADDING : paperLeft(panelLeft);
    }

    public static int innerPanelRight(int panelRight, GuideTheme theme) {
        return theme.usesListScroll() ? panelRight - SIDE_PADDING : paperRight(panelRight);
    }

    public static int innerContentX(int panelLeft, GuideTheme theme) {
        return theme.usesListScroll() ? panelLeft + SIDE_PADDING + 10 : paperContentX(panelLeft);
    }

    public static int innerContentWidth(GuideTheme theme) {
        return theme.usesListScroll() ? PANEL_WIDTH - SIDE_PADDING * 2 - 20 : paperContentWidth();
    }

    public static void drawBackgroundDim(GuideTheme theme, GuiGraphicsExtractor gui, int screenWidth, int screenHeight) {
        gui.fill(0, 0, screenWidth, screenHeight, theme.colorDimOverlay());
    }

    public static void drawOuterPanel(GuideTheme theme, GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2) {
        if (theme.frameStyle() == GuideTheme.FrameStyle.TABLET) {
            drawTabletFrame(gui, x1, y1, x2, y2, theme.colorPanelBg(), theme.colorAccent(), false);
            return;
        }
        if (theme.frameStyle() == GuideTheme.FrameStyle.PAPER) {
            drawPaperCover(gui, x1, y1, x2, y2);
            return;
        }
        drawBevel(gui, x1, y1, x2, y2, false, theme.colorPanelBg());
        gui.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, theme.colorPanelBg());
        gui.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, theme.colorAccent());
    }

    public static void drawInnerPanel(GuideTheme theme, GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2) {
        if (theme.frameStyle() == GuideTheme.FrameStyle.TABLET) {
            drawTabletFrame(gui, x1, y1, x2, y2, theme.colorInnerBg(), theme.colorAccent(), true);
            return;
        }
        if (theme.frameStyle() == GuideTheme.FrameStyle.PAPER) {
            drawPaperPage(theme, gui, x1, y1, x2, y2);
            return;
        }
        drawBevel(gui, x1, y1, x2, y2, true, theme.colorInnerBg());
    }

    public static void drawTitle(GuideTheme theme, Font font, GuiGraphicsExtractor gui, Component title, int centerX, int y) {
        gui.centeredText(font, title, centerX, y, theme.colorTitle());
    }

    public static void drawBodyLines(GuideTheme theme, Font font, GuiGraphicsExtractor gui, List<FormattedCharSequence> lines, int left, int top) {
        int lineY = top;
        for (FormattedCharSequence line : lines) {
            gui.text(font, line, left, lineY, theme.colorBody(), false);
            lineY += font.lineHeight + 2;
        }
    }

    public static void drawSubtitleLines(
            GuideTheme theme,
            Font font,
            GuiGraphicsExtractor gui,
            List<FormattedCharSequence> lines,
            int left,
            int top
    ) {
        int lineY = top;
        for (FormattedCharSequence line : lines) {
            gui.text(font, line, left, lineY, theme.colorSubtitle(), false);
            lineY += font.lineHeight + 2;
        }
    }

    public static void drawItemSlot(GuideTheme theme, GuiGraphicsExtractor gui, int slotX, int slotY) {
        if (theme.frameStyle() == GuideTheme.FrameStyle.TABLET) {
            gui.fill(slotX, slotY, slotX + 18, slotY + 18, theme.colorSlotInner());
            int border = theme.colorAccent();
            gui.fill(slotX, slotY, slotX + 18, slotY + 1, border);
            gui.fill(slotX, slotY, slotX + 1, slotY + 18, border);
            gui.fill(slotX, slotY + 17, slotX + 18, slotY + 18, border);
            gui.fill(slotX + 17, slotY, slotX + 18, slotY + 18, border);
            return;
        }
        if (theme.frameStyle() == GuideTheme.FrameStyle.PAPER) {
            drawPaperSlot(theme, gui, slotX, slotY);
            return;
        }
        drawBevel(gui, slotX, slotY, slotX + 18, slotY + 18, true, theme.colorInnerBg());
        gui.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, theme.colorSlotInner());
    }

    public static void drawItemStack(GuideTheme theme, GuiGraphicsExtractor gui, ItemStack stack, int slotX, int slotY) {
        drawItemSlot(theme, gui, slotX, slotY);
        if (!stack.isEmpty()) {
            gui.fakeItem(stack, slotX + 1, slotY + 1);
        }
    }

    public static int subtitleHeight(Font font, List<FormattedCharSequence> lines) {
        return wrappedLinesHeight(font, lines.size());
    }

    /** {@link GuideTheme#drawBodyLines} / drawSubtitleLines と一致する高さ */
    public static int wrappedLinesHeight(Font font, int lineCount) {
        if (lineCount <= 0) {
            return 0;
        }
        return lineCount * (font.lineHeight + 2) - 2;
    }

    public static boolean isMouseOverPanel(double mouseX, double mouseY, int panelLeft, int panelTop, int panelRight, int panelBottom) {
        return mouseX >= panelLeft && mouseX < panelRight && mouseY >= panelTop && mouseY < panelBottom;
    }

    public static int clampPanelTop(int screenHeight, int panelHeight, int preferredTop) {
        int maxTop = screenHeight - SCREEN_VERTICAL_MARGIN - panelHeight;
        return Mth.clamp(preferredTop, SCREEN_VERTICAL_MARGIN, Math.max(SCREEN_VERTICAL_MARGIN, maxTop));
    }

    public static int cappedPanelHeight(int screenHeight, int desiredHeight) {
        return Math.min(desiredHeight, screenHeight - SCREEN_VERTICAL_MARGIN * 2);
    }

    /** 暗いフッター上にページ番号を1回だけ描画（羊皮紙の罫線と重ねない） */
    public static void drawPageIndicator(
            GuiGraphicsExtractor gui,
            Font font,
            Component label,
            int centerX,
            int footerTop,
            int color
    ) {
        int y = footerTop + 4;
        int width = font.width(label);
        gui.text(font, label.getVisualOrderText(), centerX - width / 2, y, color, false);
    }

    /** タブレット向けスクロールヒント（フッター上・リストと重ねない） */
    public static void drawScrollHint(
            GuiGraphicsExtractor gui,
            Font font,
            Component label,
            int centerX,
            int footerTop,
            int color
    ) {
        drawPageIndicator(gui, font, label, centerX, footerTop, color);
    }

    public static void drawBodyLinesClipped(
            GuideTheme theme,
            Font font,
            GuiGraphicsExtractor gui,
            List<FormattedCharSequence> lines,
            int left,
            int top,
            int clipTop,
            int clipBottom
    ) {
        int lineY = top;
        for (FormattedCharSequence line : lines) {
            if (lineY + font.lineHeight > clipTop && lineY < clipBottom) {
                gui.text(font, line, left, lineY, theme.colorBody(), false);
            }
            lineY += font.lineHeight + 2;
            if (lineY >= clipBottom) {
                break;
            }
        }
    }

    /** タブレット内枠のスクロール領域上端 */
    public static int tabletScrollTop(int innerPanelTop) {
        return innerPanelTop + INNER_PANEL_INSET + TABLET_SCROLL_TOP_PADDING;
    }

    /** タブレット内枠のスクロール領域下端 */
    public static int tabletScrollBottom(int innerPanelBottom) {
        return innerPanelBottom - LIST_BOTTOM_INSET;
    }

    public static int tabletScrollHeight(int innerPanelTop, int innerPanelBottom) {
        return Math.max(0, tabletScrollBottom(innerPanelBottom) - tabletScrollTop(innerPanelTop));
    }

    public static void runWithScissor(GuiGraphicsExtractor gui, int left, int top, int right, int bottom, Runnable draw) {
        if (right <= left || bottom <= top) {
            draw.run();
            return;
        }
        gui.enableScissor(left, top, right, bottom);
        try {
            draw.run();
        } finally {
            gui.disableScissor();
        }
    }

    public static int tabletListViewportHeight(Font font, List<GuideLinkList.LinkItem> items) {
        if (items.isEmpty()) {
            return ROW_HEIGHT;
        }
        int count = Math.min(items.size(), TABLET_VISIBLE_LIST_ROWS);
        return GuideLinkList.listHeight(font, items.subList(0, count));
    }

    public static int bookItemsPerPage(int listColumns) {
        return BOOK_ITEMS_PER_PAGE * Math.max(1, listColumns);
    }

    /** ブック目次（説明＋リスト）の羊皮紙内高さ */
    public static int bookTocPaperContentHeight(
            Font font,
            List<FormattedCharSequence> descriptionLines,
            List<GuideLinkList.LinkItem> pageItems,
            int listColumns
    ) {
        int descriptionHeight = subtitleHeight(font, descriptionLines);
        int listHeight = pageItems.isEmpty() ? 0 : GuideLinkList.listHeight(font, pageItems, listColumns);
        return Math.max(
                48,
                PAPER_TOP_PADDING
                        + descriptionHeight
                        + (descriptionLines.isEmpty() ? 0 : 10)
                        + listHeight
                        + LIST_BOTTOM_INSET
                        + BOOK_TOC_BOTTOM_EXTRA
        );
    }

    /** ブック目次（説明＋リスト）の羊皮紙内高さ */
    public static int bookTocMaxPaperContentHeight(
            Font font,
            List<FormattedCharSequence> descriptionLines,
            List<GuideLinkList.LinkItem> allItems,
            int listColumns
    ) {
        if (allItems.isEmpty()) {
            return bookTocPaperContentHeight(font, descriptionLines, List.of(), listColumns);
        }
        GuideListPager pager = GuideListPager.byItemCount(allItems, bookItemsPerPage(listColumns));
        int maxHeight = 0;
        for (int page = 0; page < pager.pageCount(); page++) {
            int height = bookTocPaperContentHeight(font, descriptionLines, pager.pageItems(page), listColumns);
            maxHeight = Math.max(maxHeight, height);
        }
        return maxHeight;
    }

    /** タブレット目次の羊皮紙内高さ（スクロール表示窓） */
    public static int tabletTocPaperContentHeight(Font font, List<GuideLinkList.LinkItem> items) {
        int scrollWindow = tabletListViewportHeight(font, items);
        return Math.max(
                48,
                INNER_PANEL_INSET + TABLET_SCROLL_TOP_PADDING + scrollWindow + LIST_BOTTOM_INSET
        );
    }

    /** Codex リストのみの羊皮紙内高さ */
    public static int codexPaperContentHeight(Font font, List<GuideLinkList.LinkItem> items, boolean scrollMode) {
        if (scrollMode) {
            return tabletTocPaperContentHeight(font, items);
        }
        int pageItemCount = Math.min(BOOK_ITEMS_PER_PAGE, items.size());
        int listHeight = items.isEmpty()
                ? font.lineHeight
                : GuideLinkList.listHeight(font, items.subList(0, pageItemCount));
        return Math.max(48, LIST_CONTENT_PADDING + listHeight + LIST_BOTTOM_INSET);
    }

    /** 本テーマエントリ: ページ分割に使うコンテンツ高さ */
    public static int bookEntryPageHeight(int contentTop, int contentBottom) {
        return Math.max(48, contentBottom - contentTop);
    }

    public static int entryPanelHeight(GuideTheme theme) {
        return theme.usesListScroll() ? TABLET_ENTRY_PANEL_HEIGHT : ENTRY_PANEL_HEIGHT;
    }

    public static int listAreaHeight(int entryCount) {
        if (entryCount <= 0) {
            return ROW_HEIGHT;
        }
        return entryCount * ROW_HEIGHT + (entryCount - 1) * ROW_GAP;
    }

    public static int panelHeight(int subtitleHeight, int entryCount) {
        return HEADER_HEIGHT + subtitleHeight + 10 + listAreaHeight(entryCount) + FOOTER_HEIGHT + 8;
    }

    private static void drawPaperCover(GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2) {
        gui.fill(x1, y1, x2, y2, 0xFF3D2914);
        gui.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0xFF5C4033);
        gui.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, 0xFF8B6914);
        gui.fill(x1 + 1, y1 + 1, x1 + 2, y2 - 1, 0xFF8B6914);
        gui.fill(x1 + 1, y2 - 2, x2 - 1, y2 - 1, 0xFF2A1A10);
        gui.fill(x2 - 2, y1 + 1, x2 - 1, y2 - 1, 0xFF2A1A10);
    }

    private static void drawPaperPage(GuideTheme theme, GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2) {
        gui.fill(x1, y1, x2, y2, theme.colorInnerBg());
        gui.fill(x1, y1, x2, y1 + 1, 0x50C0A878);
        gui.fill(x1, y1, x1 + 1, y2, 0x50C0A878);
        gui.fill(x1, y2 - 1, x2, y2, 0x35FFFFFF);
        gui.fill(x2 - 1, y1, x2, y2, 0x35FFFFFF);

        int lineY = y1 + 14;
        int lineEnd = x2 - 6;
        int lineStart = x1 + 6;
        while (lineY < y2 - 8) {
            gui.fill(lineStart, lineY, lineEnd, lineY + 1, 0x18A08060);
            lineY += 11;
        }
    }

    private static void drawPaperSlot(GuideTheme theme, GuiGraphicsExtractor gui, int slotX, int slotY) {
        gui.fill(slotX, slotY, slotX + 18, slotY + 18, theme.colorSlotInner());
        gui.fill(slotX, slotY, slotX + 18, slotY + 1, 0x70C0A878);
        gui.fill(slotX, slotY, slotX + 1, slotY + 18, 0x70C0A878);
        gui.fill(slotX, slotY + 17, slotX + 18, slotY + 18, 0x40FFFFFF);
        gui.fill(slotX + 17, slotY, slotX + 18, slotY + 18, 0x40FFFFFF);
    }

    private static void drawTabletFrame(
            GuiGraphicsExtractor gui,
            int x1,
            int y1,
            int x2,
            int y2,
            int bgColor,
            int accentColor,
            boolean inset
    ) {
        gui.fill(x1, y1, x2, y2, bgColor);
        int line = inset ? 0x6000E5FF : accentColor;
        gui.fill(x1, y1, x2, y1 + 1, line);
        gui.fill(x1, y1, x1 + 1, y2, line);
        gui.fill(x1, y2 - 1, x2, y2, line);
        gui.fill(x2 - 1, y1, x2, y2, line);
        if (!inset) {
            gui.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, accentColor);
        }
    }

    private static void drawBevel(GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2, boolean sunken, int bgColor) {
        int topLeftColor = sunken ? 0xFF1F1F1F : 0xFF5F5F5F;
        int bottomRightColor = sunken ? 0xFF4A4A4A : 0xFF1F1F1F;
        gui.fill(x1, y1, x2, y2, bgColor);
        gui.fill(x1, y1, x2, y1 + 1, topLeftColor);
        gui.fill(x1, y1, x1 + 1, y2, topLeftColor);
        gui.fill(x1, y2 - 1, x2, y2, bottomRightColor);
        gui.fill(x2 - 1, y1, x2, y2, bottomRightColor);
    }
}
