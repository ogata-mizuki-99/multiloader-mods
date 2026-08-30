package com.ogatamizuki.guide.client.screen;

import com.ogatamizuki.guide.GuideBookRegistry;
import com.ogatamizuki.guide.GuideThemeRegistry;
import com.ogatamizuki.guide.client.GuideLibClient;
import com.ogatamizuki.guide.client.page.GuideEntryScrollContent;
import com.ogatamizuki.guide.client.page.GuidePageExpander;
import com.ogatamizuki.guide.client.page.PageRenderer;
import com.ogatamizuki.guide.client.page.PageRenderers;
import com.ogatamizuki.guide.model.GuideBook;
import com.ogatamizuki.guide.model.GuideEntry;
import com.ogatamizuki.guide.model.GuidePage;
import com.ogatamizuki.guide.model.GuideTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public class GuideEntryScreen extends Screen {
    private final Screen parentScreen;
    private final Identifier bookId;
    private final String entryId;
    private final GuideTheme theme;
    private final Component displayTitle;
    private GuideEntry entry;
    private List<GuidePage> entryPages = List.of();
    private List<GuidePage> displayPages = List.of();
    private final boolean closeOnBack;
    private int pageIndex;
    private final GuideLinkList footerLinks = new GuideLinkList();
    private final GuideScrollPanel contentScroll = new GuideScrollPanel();

    private int panelLeft;
    private int panelTop;
    private int panelRight;
    private int panelBottom;
    private int panelHeight;
    private int contentTop;
    private int contentBottom;
    private int contentScrollTop;
    private int contentScrollBottom;
    private int contentWidth;
    private int footerTop;

    public GuideEntryScreen(Screen parentScreen, Identifier bookId, String entryId) {
        this(parentScreen, bookId, entryId, null, false);
    }

    public GuideEntryScreen(Screen parentScreen, Identifier bookId, String entryId, GuideTheme themeOverride) {
        this(parentScreen, bookId, entryId, themeOverride, false);
    }

    public GuideEntryScreen(Screen parentScreen, Identifier bookId, String entryId, GuideTheme themeOverride, boolean closeOnBack) {
        super(Component.empty());
        this.parentScreen = parentScreen;
        this.bookId = bookId;
        this.entryId = entryId;
        this.closeOnBack = closeOnBack;

        GuideBook book = GuideBookRegistry.getBook(bookId);
        GuideEntry loaded = book != null ? book.getEntry(entryId) : null;
        this.theme = themeOverride != null
                ? themeOverride
                : GuideThemeRegistry.resolve(book != null ? book.themeId() : GuideTheme.BOOK_ID);
        this.displayTitle = loaded != null
                ? GuideLibClient.translateOrLiteral(loaded.nameKey())
                : Component.translatable("guide_lib.screen.entry_missing");
    }

    @Override
    protected void init() {
        super.init();
        GuideBook book = GuideBookRegistry.getBook(bookId);
        this.entry = book != null ? book.getEntry(entryId) : null;
        computeLayout();
        this.entryPages = entry != null ? entry.pages() : List.of();
        this.displayPages = buildDisplayPages();
        if (pageIndex >= pageCount()) {
            pageIndex = Math.max(0, pageCount() - 1);
        }
        contentScroll.reset();
    }

    private void computeLayout() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.panelHeight = GuideUi.cappedPanelHeight(this.height, GuideUi.entryPanelHeight(theme));
        this.panelLeft = centerX - GuideUi.PANEL_WIDTH / 2;
        this.panelRight = centerX + GuideUi.PANEL_WIDTH / 2;
        this.panelTop = GuideUi.clampPanelTop(this.height, panelHeight, centerY - panelHeight / 2);
        this.panelBottom = panelTop + panelHeight;
        this.contentTop = panelTop + GuideUi.HEADER_HEIGHT + 8;
        this.footerTop = panelBottom - GuideUi.FOOTER_HEIGHT;
        this.contentBottom = footerTop - GuideUi.LIST_BOTTOM_INSET;
        if (theme.usesListScroll()) {
            this.contentScrollTop = GuideUi.tabletScrollTop(contentTop);
            this.contentScrollBottom = GuideUi.tabletScrollBottom(contentBottom);
        } else {
            this.contentScrollTop = contentTop;
            this.contentScrollBottom = contentBottom;
        }
        this.contentWidth = GuideUi.innerContentWidth(theme);
    }

    private List<GuidePage> buildDisplayPages() {
        if (entry == null || entry.pages().isEmpty()) {
            return List.of();
        }
        if (theme.usesListScroll()) {
            return entry.pages();
        }
        int contentHeight = GuideUi.bookEntryPageHeight(contentTop, contentBottom);
        return GuidePageExpander.expand(entry.pages(), this.font, contentWidth, contentHeight);
    }

    private int pageCount() {
        return displayPages.size();
    }

    private int contentViewportHeight() {
        return contentScrollBottom - contentScrollTop;
    }

    private int contentDrawTop() {
        return theme.usesListScroll() ? contentScrollTop : contentTop;
    }

    private int scrollContentHeight() {
        return GuideEntryScrollContent.totalHeight(
                this.font,
                theme,
                entryPages,
                contentWidth,
                contentViewportHeight()
        );
    }

    private boolean showEntryScrollHint() {
        return theme.usesListScroll()
                && !entryPages.isEmpty()
                && contentScroll.isScrollable(scrollContentHeight(), contentViewportHeight());
    }

    private int footerLinkY() {
        if (showEntryScrollHint() || (!theme.usesListScroll() && pageCount() > 1)) {
            return footerTop + 16;
        }
        return footerTop + 4;
    }

    private void goBack() {
        if (parentScreen != null) {
            Minecraft.getInstance().setScreen(parentScreen);
        } else {
            Minecraft.getInstance().setScreen(new GuideBookScreen(null, bookId, theme, closeOnBack));
        }
    }

    private void previousPage() {
        if (pageIndex > 0) {
            pageIndex--;
            contentScroll.reset();
            GuideLibClient.playPageTurnSound(theme);
        }
    }

    private void nextPage() {
        if (pageIndex < pageCount() - 1) {
            pageIndex++;
            contentScroll.reset();
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
            goBack();
            return true;
        }
        if (!theme.usesListScroll()) {
            if (GuideUi.isPrevPageKey(event)) {
                previousPage();
                return true;
            }
            if (GuideUi.isNextPageKey(event)) {
                nextPage();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!GuideUi.isMouseOverPanel(mouseX, mouseY, panelLeft, panelTop, panelRight, panelBottom)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        if (theme.usesListScroll() && !entryPages.isEmpty()) {
            if (contentScroll.scroll(scrollY, scrollContentHeight(), contentViewportHeight())) {
                return true;
            }
        }

        if (!theme.usesListScroll() && pageCount() > 1) {
            if (scrollY > 0) {
                previousPage();
            } else if (scrollY < 0) {
                nextPage();
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (footerLinks.mouseClicked(event)) {
            return true;
        }
        if (entry != null && !entryPages.isEmpty()) {
            int contentX = GuideUi.innerContentX(panelLeft, theme);
            if (theme.usesListScroll()) {
                if (GuideEntryScrollContent.mouseClicked(
                        this.font,
                        theme,
                        entryPages,
                        contentX,
                        contentDrawTop(),
                        contentWidth,
                        contentViewportHeight(),
                        contentScroll.scrollOffset(),
                        event
                )) {
                    return true;
                }
            } else {
                GuidePage page = displayPages.get(pageIndex);
                PageRenderer renderer = PageRenderers.get(page.type());
                if (renderer.mouseClicked(
                        null,
                        theme,
                        page,
                        contentX,
                        contentTop,
                        contentWidth,
                        contentViewportHeight(),
                        event
                )) {
                    return true;
                }
            }
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
        footerLinks.clear();

        if (entry != null) {
            GuideUi.drawTitle(theme, this.font, gui, displayTitle, centerX, panelTop + 6);
            GuideUi.drawInnerPanel(
                    theme,
                    gui,
                    GuideUi.innerPanelLeft(panelLeft, theme),
                    contentTop,
                    GuideUi.innerPanelRight(panelRight, theme),
                    contentBottom
            );

            if (!entryPages.isEmpty()) {
                int contentX = GuideUi.innerContentX(panelLeft, theme);
                if (theme.usesListScroll()) {
                    int scrollLeft = GuideUi.innerPanelLeft(panelLeft, theme);
                    int scrollRight = GuideUi.innerPanelRight(panelRight, theme);
                    GuideUi.runWithScissor(
                            gui,
                            scrollLeft,
                            contentScrollTop,
                            scrollRight,
                            contentScrollBottom,
                            () -> GuideEntryScrollContent.render(
                                    gui,
                                    this.font,
                                    theme,
                                    entryPages,
                                    contentX,
                                    contentDrawTop(),
                                    contentWidth,
                                    contentViewportHeight(),
                                    contentScrollTop,
                                    contentScrollBottom,
                                    contentScroll.scrollOffset(),
                                    mouseX,
                                    mouseY
                            )
                    );
                    if (showEntryScrollHint()) {
                        Component hint = Component.translatable("guide_lib.screen.scroll_hint");
                        GuideUi.drawScrollHint(gui, this.font, hint, centerX, footerTop, theme.colorMuted());
                    }
                } else if (pageCount() > 0) {
                    GuidePage page = displayPages.get(pageIndex);
                    PageRenderer renderer = PageRenderers.get(page.type());
                    renderer.render(
                            gui,
                            theme,
                            page,
                            contentX,
                            contentTop,
                            contentWidth,
                            contentViewportHeight(),
                            mouseX,
                            mouseY,
                            0
                    );

                    if (pageCount() > 1) {
                        Component pageLabel = Component.translatable(
                                "guide_lib.screen.page_indicator",
                                pageIndex + 1,
                                pageCount()
                        );
                        GuideUi.drawPageIndicator(gui, this.font, pageLabel, centerX, footerTop, theme.colorMuted());
                    }
                }
            } else {
                Component empty = Component.translatable("guide_lib.screen.entry_empty");
                gui.centeredText(this.font, empty, centerX, contentTop + 20, theme.colorMuted());
            }
        } else {
            gui.centeredText(this.font, displayTitle, centerX, panelTop + 40, theme.colorError());
        }

        renderFooterLinks(gui, centerX, mouseX, mouseY);

        super.extractRenderState(gui, mouseX, mouseY, partialTick);
    }

    private void renderFooterLinks(GuiGraphicsExtractor gui, int centerX, int mouseX, int mouseY) {
        int linkY = footerLinkY();
        if (!theme.usesListScroll() && pageCount() > 0 && pageIndex > 0) {
            footerLinks.render(
                    gui,
                    this.font,
                    theme,
                    panelLeft + GuideUi.SIDE_PADDING,
                    linkY,
                    contentWidth,
                    List.of(GuideLinkList.LinkItem.plain(Component.translatable("guide_lib.link.prev"), this::previousPage)),
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

        if (!theme.usesListScroll() && pageCount() > 0 && pageIndex < pageCount() - 1) {
            Component nextLabel = Component.translatable("guide_lib.link.next");
            int nextWidth = this.font.width(nextLabel);
            footerLinks.render(
                    gui,
                    this.font,
                    theme,
                    panelRight - GuideUi.SIDE_PADDING - nextWidth,
                    linkY,
                    nextWidth,
                    List.of(GuideLinkList.LinkItem.plain(nextLabel, this::nextPage)),
                    mouseX,
                    mouseY,
                    GuideLinkList.LinkSurface.PANEL
            );
        }
    }
}
