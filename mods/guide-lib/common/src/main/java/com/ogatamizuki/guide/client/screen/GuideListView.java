package com.ogatamizuki.guide.client.screen;

import com.ogatamizuki.guide.model.GuideTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Codex / ブック目次の一覧ナビゲーション。
 * 本テーマ ({@link GuideTheme.FrameStyle#PAPER} 等) はページ送り、タブレットはスクロール。
 */
public final class GuideListView {
    private final GuideScrollPanel scrollPanel = new GuideScrollPanel();
    private GuideListPager pager;
    private List<GuideLinkList.LinkItem> allItems = List.of();
    private int viewportHeight;
    private int scrollPrefixHeight;
    private int listColumns = 1;
    private boolean scrollMode;

    public void configure(GuideTheme theme, List<GuideLinkList.LinkItem> items, Font font, int viewportHeight) {
        configure(theme, items, font, viewportHeight, 0, 1);
    }

    public void configure(
            GuideTheme theme,
            List<GuideLinkList.LinkItem> items,
            Font font,
            int viewportHeight,
            int scrollPrefixHeight
    ) {
        configure(theme, items, font, viewportHeight, scrollPrefixHeight, 1);
    }

    public void configure(
            GuideTheme theme,
            List<GuideLinkList.LinkItem> items,
            Font font,
            int viewportHeight,
            int scrollPrefixHeight,
            int listColumns
    ) {
        this.allItems = items;
        this.viewportHeight = viewportHeight;
        this.scrollPrefixHeight = scrollPrefixHeight;
        this.listColumns = Math.max(1, listColumns);
        this.scrollMode = theme.usesListScroll();
        scrollPanel.reset();
        if (scrollMode) {
            pager = null;
        } else if (viewportHeight > 0) {
            pager = GuideListPager.byHeight(items, font, viewportHeight, this.listColumns);
        } else {
            pager = GuideListPager.byItemCount(items, GuideUi.bookItemsPerPage(this.listColumns));
        }
    }

    public int listColumns() {
        return listColumns;
    }

    public int maxBookTocPaperContentHeight(Font font, List<FormattedCharSequence> descriptionLines) {
        if (pager == null) {
            return GuideUi.bookTocPaperContentHeight(font, descriptionLines, List.of(), listColumns);
        }
        int maxHeight = 0;
        for (int page = 0; page < pager.pageCount(); page++) {
            int height = GuideUi.bookTocPaperContentHeight(font, descriptionLines, pager.pageItems(page), listColumns);
            maxHeight = Math.max(maxHeight, height);
        }
        return maxHeight;
    }

    public void reset() {
        scrollPanel.reset();
        if (pager != null) {
            pager.reset();
        }
    }

    public boolean isScrollMode() {
        return scrollMode;
    }

    public int scrollPrefixHeight() {
        return scrollPrefixHeight;
    }

    public List<GuideLinkList.LinkItem> visibleItems() {
        return scrollMode ? allItems : pager.currentItems();
    }

    public int scrollOffset() {
        return scrollMode ? scrollPanel.scrollOffset() : 0;
    }

    public int contentHeight(Font font) {
        int listHeight = allItems.isEmpty() ? 0 : GuideLinkList.listHeight(font, allItems, listColumns);
        int bottomPad = scrollMode ? GuideUi.SCROLL_LIST_BOTTOM_PADDING : 0;
        return scrollPrefixHeight + listHeight + bottomPad;
    }

    public boolean handleMouseScroll(double scrollY, Font font) {
        if (scrollMode) {
            return scrollPanel.scroll(scrollY, contentHeight(font), viewportHeight);
        }
        if (scrollY > 0) {
            return previousPage();
        }
        if (scrollY < 0) {
            return nextPage();
        }
        return false;
    }

    public boolean previousPage() {
        return pager != null && pager.previousPage();
    }

    public boolean nextPage() {
        return pager != null && pager.nextPage();
    }

    public boolean showScrollHint(Font font) {
        return scrollMode && scrollPanel.isScrollable(contentHeight(font), viewportHeight);
    }

    public boolean showPageNav() {
        return !scrollMode && pager != null && pager.pageCount() > 1;
    }

    public boolean supportsWheelPaging() {
        return !scrollMode && pager != null && pager.pageCount() > 1;
    }

    public int pageIndex() {
        return pager == null ? 0 : pager.pageIndex();
    }

    public int pageCount() {
        return pager == null ? 1 : pager.pageCount();
    }
}
