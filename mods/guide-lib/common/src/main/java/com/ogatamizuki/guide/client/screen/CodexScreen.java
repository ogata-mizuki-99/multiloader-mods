package com.ogatamizuki.guide.client.screen;

import com.ogatamizuki.guide.GuideBookRegistry;
import com.ogatamizuki.guide.client.GuideLibClient;
import com.ogatamizuki.guide.model.GuideBook;
import com.ogatamizuki.guide.model.GuideTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class CodexScreen extends Screen {
    private final Screen parentScreen;
    private final GuideTheme theme;
    private List<GuideBook> books = List.of();
    private List<GuideLinkList.LinkItem> listItems = List.of();
    private final GuideLinkList bookLinks = new GuideLinkList();
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

    public CodexScreen(Screen parentScreen) {
        this(parentScreen, GuideTheme.bookBuiltin());
    }

    public CodexScreen(Screen parentScreen, GuideTheme theme) {
        super(Component.translatable("guide_lib.screen.codex.title"));
        this.parentScreen = parentScreen;
        this.theme = theme != null ? theme : GuideTheme.bookBuiltin();
    }

    @Override
    protected void init() {
        super.init();
        this.books = GuideBookRegistry.getCodexBooks();
        this.listItems = buildListItems();
        layoutListView();
    }

    private void layoutListView() {
        computeLayout(false);
        listView.configure(theme, listItems, this.font, listViewportHeight);
        computeLayout(true);
        listView.configure(theme, listItems, this.font, listViewportHeight);
        computeLayout(true);
    }

    private List<GuideLinkList.LinkItem> buildListItems() {
        List<GuideLinkList.LinkItem> items = new ArrayList<>();
        for (GuideBook book : books) {
            Component label = GuideLibClient.translateOrLiteral(book.nameKey());
            GuideBook targetBook = book;
            if (!book.icon().isEmpty()) {
                items.add(GuideLinkList.LinkItem.withIcon(label, book.icon(), () -> openBook(targetBook)));
            } else {
                items.add(GuideLinkList.LinkItem.of(label, () -> openBook(targetBook)));
            }
        }
        return items;
    }

    private void computeLayout(boolean useConfiguredPager) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int paperContentHeight;
        if (theme.usesListScroll()) {
            paperContentHeight = GuideUi.codexPaperContentHeight(this.font, listItems, true);
        } else if (useConfiguredPager) {
            paperContentHeight = listView.maxBookTocPaperContentHeight(this.font, List.of());
        } else {
            paperContentHeight = GuideUi.codexPaperContentHeight(this.font, listItems, false);
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
        this.listStartY = this.paperTop + GuideUi.LIST_CONTENT_PADDING;
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

    private void openBook(GuideBook book) {
        Minecraft.getInstance().setScreen(new GuideBookScreen(this, book.id(), theme));
    }

    private void openHelp() {
        Minecraft.getInstance().setScreen(new GuideBookScreen(this, Identifier.parse("guide_lib:help"), theme));
    }

    private void closeScreen() {
        if (parentScreen != null) {
            Minecraft.getInstance().setScreen(parentScreen);
        } else {
            Minecraft.getInstance().setScreen(null);
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

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            closeScreen();
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
        if (bookLinks.mouseClicked(event) || footerLinks.mouseClicked(event)) {
            return true;
        }
        if (GuideUi.isRightClick(event)) {
            closeScreen();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        GuideUi.drawBackgroundDim(theme, gui, this.width, this.height);
        GuideUi.drawOuterPanel(theme, gui, panelLeft, panelTop, panelRight, panelBottom);

        int centerX = this.width / 2;
        bookLinks.clear();
        footerLinks.clear();
        GuideUi.drawTitle(theme, this.font, gui, this.title, centerX, panelTop + 6);

        if (books.isEmpty()) {
            Component empty = Component.translatable("guide_lib.screen.codex.empty");
            gui.centeredText(this.font, empty, centerX, paperTop + 20, theme.colorMuted());
        } else {
            GuideUi.drawInnerPanel(
                    theme,
                    gui,
                    GuideUi.innerPanelLeft(panelLeft, theme),
                    paperTop,
                    GuideUi.innerPanelRight(panelRight, theme),
                    paperBottom
            );

            int scrollLeft = GuideUi.innerPanelLeft(panelLeft, theme);
            int scrollRight = GuideUi.innerPanelRight(panelRight, theme);
            if (listView.isScrollMode()) {
                GuideUi.runWithScissor(gui, scrollLeft, listScrollTop, scrollRight, listScrollBottom, () -> bookLinks.render(
                        gui,
                        this.font,
                        theme,
                        contentX,
                        listScrollTop - listView.scrollOffset(),
                        contentWidth,
                        listView.visibleItems(),
                        mouseX,
                        mouseY,
                        GuideLinkList.LinkSurface.CONTENT,
                        0,
                        listScrollTop,
                        listScrollBottom
                ));
            } else {
                int paperLeft = GuideUi.innerPanelLeft(panelLeft, theme);
                int paperRight = GuideUi.innerPanelRight(panelRight, theme);
                int paperClipBottom = paperBottom - GuideUi.PAPER_BOTTOM_CLIP_INSET;
                GuideUi.runWithScissor(gui, paperLeft + 1, paperTop + 1, paperRight - 1, paperClipBottom, () -> bookLinks.render(
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
                        paperClipBottom
                ));
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
        }

        renderFooterLinks(gui, centerX, mouseX, mouseY);

        super.extractRenderState(gui, mouseX, mouseY, partialTick);
    }

    private void renderFooterLinks(GuiGraphicsExtractor gui, int centerX, int mouseX, int mouseY) {
        int linkY = footerLinkY();
        footerLinks.render(
                gui,
                this.font,
                theme,
                panelLeft + GuideUi.SIDE_PADDING,
                linkY,
                this.font.width("?") + 4,
                List.of(GuideLinkList.LinkItem.plain(Component.literal("?"), this::openHelp)),
                mouseX,
                mouseY,
                GuideLinkList.LinkSurface.PANEL
        );

        if (listView.showPageNav() && listView.pageIndex() > 0) {
            footerLinks.render(
                    gui,
                    this.font,
                    theme,
                    panelLeft + GuideUi.SIDE_PADDING + this.font.width("?") + 12,
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
                this::closeScreen,
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
