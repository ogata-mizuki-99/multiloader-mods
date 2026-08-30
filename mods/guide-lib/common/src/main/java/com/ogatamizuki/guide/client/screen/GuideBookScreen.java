package com.ogatamizuki.guide.client.screen;

import com.ogatamizuki.guide.GuideBookRegistry;
import com.ogatamizuki.guide.GuideThemeRegistry;
import com.ogatamizuki.guide.client.GuideLibClient;
import com.ogatamizuki.guide.client.text.GuideTextLayout;
import com.ogatamizuki.guide.model.GuideBook;
import com.ogatamizuki.guide.model.GuideCategory;
import com.ogatamizuki.guide.model.GuideEntry;
import com.ogatamizuki.guide.model.GuideTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class GuideBookScreen extends Screen {
    private final Screen parentScreen;
    private final Identifier bookId;
    private final GuideTheme theme;
    private final Component displayTitle;
    private final boolean closeOnBack;
    private GuideBook book;
    private List<GuideLinkList.LinkItem> listItems = List.of();
    private List<FormattedCharSequence> descriptionLines = List.of();
    private final GuideLinkList entryLinks = new GuideLinkList();
    private final GuideLinkList footerLinks = new GuideLinkList();
    private final GuideListView listView = new GuideListView();

    private int panelLeft;
    private int panelTop;
    private int panelRight;
    private int panelBottom;
    private int paperTop;
    private int paperBottom;
    private int contentX;
    private int contentWidth;
    private int listStartY;
    private int listScrollTop;
    private int listScrollBottom;
    private int listClipBottom;
    private int listViewportHeight;
    private int footerTop;
    private int footerY;

    public GuideBookScreen(Screen parentScreen, Identifier bookId) {
        this(parentScreen, bookId, null, false);
    }

    public GuideBookScreen(Screen parentScreen, Identifier bookId, GuideTheme themeOverride) {
        this(parentScreen, bookId, themeOverride, false);
    }

    public GuideBookScreen(Screen parentScreen, Identifier bookId, GuideTheme themeOverride, boolean closeOnBack) {
        super(Component.empty());
        this.parentScreen = parentScreen;
        this.bookId = bookId;
        this.closeOnBack = closeOnBack;
        GuideBook loaded = GuideBookRegistry.getBook(bookId);
        this.theme = themeOverride != null
                ? themeOverride
                : GuideThemeRegistry.resolve(loaded != null ? loaded.themeId() : GuideTheme.BOOK_ID);
        this.displayTitle = loaded != null
                ? GuideLibClient.translateOrLiteral(loaded.nameKey())
                : Component.translatable("guide_lib.screen.book_missing");
    }

    @Override
    protected void init() {
        super.init();
        this.book = GuideBookRegistry.getBook(bookId);
        this.descriptionLines = List.of();
        this.listItems = buildListItems();
        if (this.book != null && book.descriptionKey() != null && !book.descriptionKey().isEmpty()) {
            Component desc = GuideLibClient.translateOrLiteral(book.descriptionKey());
            this.descriptionLines = GuideTextLayout.split(this.font, desc, GuideUi.innerContentWidth(theme));
        }
        layoutListView();
    }

    private int listColumns = 1;

    private void layoutListView() {
        computeLayout(false);
        int prefixHeight = descriptionLines.isEmpty()
                ? 0
                : GuideUi.subtitleHeight(this.font, descriptionLines) + 10;
        listColumns = book != null ? book.listColumns() : 1;
        listView.configure(theme, listItems, this.font, listViewportHeight, prefixHeight, listColumns);
        computeLayout(true);
        listView.configure(theme, listItems, this.font, listViewportHeight, prefixHeight, listColumns);
        computeLayout(true);
    }

    private List<GuideLinkList.LinkItem> buildListItems() {
        List<GuideLinkList.LinkItem> items = new ArrayList<>();
        if (book == null) {
            return items;
        }

        boolean showCategoryHeaders = shouldShowCategoryHeaders(book);
        for (GuideCategory category : book.categories()) {
            if (showCategoryHeaders && category.nameKey() != null && !category.nameKey().isEmpty()) {
                items.add(GuideLinkList.LinkItem.heading(GuideLibClient.translateOrLiteral(category.nameKey())));
            }
            for (String entryId : category.entryIds()) {
                GuideEntry entry = book.getEntry(entryId);
                if (entry == null) {
                    continue;
                }
                Component label = GuideLibClient.translateOrLiteral(entry.nameKey());
                GuideEntry targetEntry = entry;
                if (!entry.icon().isEmpty()) {
                    items.add(GuideLinkList.LinkItem.withIcon(label, entry.icon(), () -> openEntry(targetEntry)));
                } else {
                    items.add(GuideLinkList.LinkItem.of(label, () -> openEntry(targetEntry)));
                }
            }
        }
        return items;
    }

    private static boolean shouldShowCategoryHeaders(GuideBook book) {
        if (book.categories().size() > 1) {
            return true;
        }
        if (book.categories().isEmpty()) {
            return false;
        }
        GuideCategory category = book.categories().get(0);
        return !"default".equals(category.id()) && category.nameKey() != null && !category.nameKey().isEmpty();
    }

    private void computeLayout(boolean useConfiguredPager) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int paperContentHeight;
        if (theme.usesListScroll()) {
            paperContentHeight = GuideUi.tabletTocPaperContentHeight(this.font, listItems);
        } else if (useConfiguredPager) {
            paperContentHeight = listView.maxBookTocPaperContentHeight(this.font, descriptionLines);
        } else {
            paperContentHeight = GuideUi.bookTocMaxPaperContentHeight(this.font, descriptionLines, listItems, listColumns);
        }
        int panelHeight = GuideUi.HEADER_HEIGHT + 12 + paperContentHeight + GuideUi.FOOTER_HEIGHT + 10;
        panelHeight = GuideUi.cappedPanelHeight(this.height, panelHeight);

        this.panelLeft = centerX - GuideUi.PANEL_WIDTH / 2;
        this.panelRight = centerX + GuideUi.PANEL_WIDTH / 2;
        this.panelTop = GuideUi.clampPanelTop(this.height, panelHeight, centerY - panelHeight / 2);
        this.panelBottom = this.panelTop + panelHeight;
        this.paperTop = this.panelTop + GuideUi.HEADER_HEIGHT + 8;
        this.paperBottom = this.panelBottom - GuideUi.FOOTER_HEIGHT - 6;
        this.contentX = GuideUi.innerContentX(this.panelLeft, theme);
        this.contentWidth = GuideUi.innerContentWidth(theme);
        int descriptionHeight = GuideUi.subtitleHeight(this.font, descriptionLines);
        this.listStartY = this.paperTop + GuideUi.PAPER_TOP_PADDING + descriptionHeight + (descriptionLines.isEmpty() ? 0 : 10);
        this.listScrollTop = GuideUi.tabletScrollTop(this.paperTop);
        this.listScrollBottom = GuideUi.tabletScrollBottom(this.paperBottom);
        this.listClipBottom = theme.usesListScroll()
                ? this.listScrollBottom
                : this.paperBottom - GuideUi.PAPER_BOTTOM_CLIP_INSET;
        if (theme.usesListScroll()) {
            this.listViewportHeight = GuideUi.tabletScrollHeight(this.paperTop, this.paperBottom);
        } else {
            this.listViewportHeight = Math.max(0, this.listClipBottom - this.listStartY);
        }
        this.footerTop = this.panelBottom - GuideUi.FOOTER_HEIGHT;
        this.footerY = this.footerTop + 4;
    }

    private int footerLinkY() {
        if (listView.showPageNav() || listView.showScrollHint(this.font)) {
            return footerTop + 16;
        }
        return footerY;
    }

    private void openEntry(GuideEntry entry) {
        Minecraft.getInstance().setScreen(new GuideEntryScreen(this, bookId, entry.id(), theme, closeOnBack));
    }

    private void goBack() {
        if (parentScreen != null) {
            Minecraft.getInstance().setScreen(parentScreen);
        } else if (closeOnBack) {
            Minecraft.getInstance().setScreen(null);
        } else {
            Minecraft.getInstance().setScreen(new CodexScreen(null, theme));
        }
    }

    private void previousListPage() {
        if (listView.previousPage()) {
            GuideLibClient.playPageTurnSound(theme);
        }
    }

    private void nextListPage() {
        if (listView.nextPage()) {
            GuideLibClient.playPageTurnSound(theme);
        }
    }

    public GuideTheme theme() {
        return theme;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            goBack();
            return true;
        }
        if (!listView.isScrollMode()) {
            if (GuideUi.isPrevPageKey(event)) {
                previousListPage();
                return true;
            }
            if (GuideUi.isNextPageKey(event)) {
                nextListPage();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (listView.supportsWheelPaging() && GuideUi.isMouseOverPanel(mouseX, mouseY, panelLeft, panelTop, panelRight, panelBottom)) {
            if (listView.handleMouseScroll(scrollY, this.font)) {
                GuideLibClient.playPageTurnSound(theme);
            }
            return true;
        }
        if (listView.isScrollMode() && GuideUi.isMouseOver(
                mouseX,
                mouseY,
                panelLeft + GuideUi.SIDE_PADDING,
                listScrollTop,
                GuideUi.PANEL_WIDTH - GuideUi.SIDE_PADDING * 2,
                listScrollBottom - listScrollTop
        )) {
            return listView.handleMouseScroll(scrollY, this.font);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (entryLinks.mouseClicked(event) || footerLinks.mouseClicked(event)) {
            return true;
        }
        if (GuideUi.isRightClick(event)) {
            goBack();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        GuideUi.drawBackgroundDim(theme, gui, this.width, this.height);
        GuideUi.drawOuterPanel(theme, gui, panelLeft, panelTop, panelRight, panelBottom);

        int centerX = this.width / 2;
        entryLinks.clear();
        footerLinks.clear();
        if (book != null) {
            GuideUi.drawTitle(theme, this.font, gui, displayTitle, centerX, panelTop + 6);
            GuideUi.drawInnerPanel(
                    theme,
                    gui,
                    GuideUi.innerPanelLeft(panelLeft, theme),
                    paperTop,
                    GuideUi.innerPanelRight(panelRight, theme),
                    paperBottom
            );

            int scrollOff = listView.scrollOffset();
            int scrollLeft = GuideUi.innerPanelLeft(panelLeft, theme);
            int scrollRight = GuideUi.innerPanelRight(panelRight, theme);

            if (listView.isScrollMode()) {
                GuideUi.runWithScissor(gui, scrollLeft, listScrollTop, scrollRight, listScrollBottom, () -> {
                    if (!descriptionLines.isEmpty()) {
                        GuideUi.drawBodyLinesClipped(
                                theme,
                                this.font,
                                gui,
                                descriptionLines,
                                contentX,
                                listScrollTop - scrollOff,
                                listScrollTop,
                                listScrollBottom
                        );
                    }
                    entryLinks.render(
                            gui,
                            this.font,
                            theme,
                            contentX,
                            listScrollTop + listView.scrollPrefixHeight() - scrollOff,
                            contentWidth,
                            listView.visibleItems(),
                            mouseX,
                            mouseY,
                            GuideLinkList.LinkSurface.CONTENT,
                            0,
                            listScrollTop,
                            listScrollBottom,
                            listColumns
                    );
                });
            } else {
                int paperLeft = GuideUi.innerPanelLeft(panelLeft, theme);
                int paperRight = GuideUi.innerPanelRight(panelRight, theme);
                int paperClipBottom = paperBottom - GuideUi.PAPER_BOTTOM_CLIP_INSET;
                GuideUi.runWithScissor(gui, paperLeft + 1, paperTop + 1, paperRight - 1, paperClipBottom, () -> {
                    int lineY = paperTop + GuideUi.PAPER_TOP_PADDING;
                    if (!descriptionLines.isEmpty()) {
                        GuideUi.drawBodyLines(theme, this.font, gui, descriptionLines, contentX, lineY);
                    }
                    entryLinks.render(
                            gui,
                            this.font,
                            theme,
                            contentX,
                            listStartY,
                            contentWidth,
                            listView.visibleItems(),
                            mouseX,
                            mouseY,
                            GuideLinkList.LinkSurface.CONTENT,
                            0,
                            paperTop,
                            paperClipBottom,
                            listColumns
                    );
                });
            }

            if (listView.showScrollHint(this.font)) {
                Component hint = Component.translatable("guide_lib.screen.scroll_hint");
                GuideUi.drawScrollHint(gui, this.font, hint, centerX, footerTop, theme.colorMuted());
            } else if (listView.showPageNav()) {
                Component pageLabel = Component.translatable(
                        "guide_lib.screen.page_indicator",
                        listView.pageIndex() + 1,
                        listView.pageCount()
                );
                GuideUi.drawPageIndicator(gui, this.font, pageLabel, centerX, footerTop, theme.colorMuted());
            }

            renderFooterLinks(gui, centerX, mouseX, mouseY);
        } else {
            gui.centeredText(this.font, displayTitle, centerX, panelTop + 40, theme.colorError());
            footerLinks.renderCentered(
                    gui,
                    this.font,
                    theme,
                    centerX,
                    footerY,
                    Component.translatable("guide_lib.link.back"),
                    this::goBack,
                    mouseX,
                    mouseY,
                    GuideLinkList.LinkSurface.PANEL
            );
        }

        super.extractRenderState(gui, mouseX, mouseY, partialTick);
    }

    private void renderFooterLinks(GuiGraphicsExtractor gui, int centerX, int mouseX, int mouseY) {
        int linkY = footerLinkY();
        if (listView.showPageNav() && listView.pageIndex() > 0) {
            footerLinks.render(
                    gui,
                    this.font,
                    theme,
                    panelLeft + GuideUi.SIDE_PADDING,
                    linkY,
                    contentWidth(),
                    List.of(GuideLinkList.LinkItem.plain(Component.translatable("guide_lib.link.prev"), this::previousListPage)),
                    mouseX,
                    mouseY,
                    GuideLinkList.LinkSurface.PANEL
            );
        }

        footerLinks.renderCentered(
                gui,
                this.font,
                theme,
                centerX,
                linkY,
                Component.translatable("guide_lib.link.back"),
                this::goBack,
                mouseX,
                mouseY,
                GuideLinkList.LinkSurface.PANEL
        );

        if (listView.showPageNav() && listView.pageIndex() < listView.pageCount() - 1) {
            Component nextLabel = Component.translatable("guide_lib.link.next");
            int nextWidth = this.font.width(nextLabel);
            footerLinks.render(
                    gui,
                    this.font,
                    theme,
                    panelRight - GuideUi.SIDE_PADDING - nextWidth,
                    linkY,
                    nextWidth,
                    List.of(GuideLinkList.LinkItem.plain(nextLabel, this::nextListPage)),
                    mouseX,
                    mouseY,
                    GuideLinkList.LinkSurface.PANEL
            );
        }
    }

    private int contentWidth() {
        return GuideUi.PANEL_WIDTH - GuideUi.SIDE_PADDING * 2;
    }
}
