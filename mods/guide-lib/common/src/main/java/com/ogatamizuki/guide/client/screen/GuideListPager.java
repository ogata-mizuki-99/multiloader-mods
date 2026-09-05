package com.ogatamizuki.guide.client.screen;

import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.List;

/**
 * 本テーマ向け: 目次を固定件数でページ分割する。
 */
public final class GuideListPager {
    private static final int PACK_SAFETY = 4;

    private final List<List<GuideLinkList.LinkItem>> pages;
    private int pageIndex;

    private GuideListPager(List<List<GuideLinkList.LinkItem>> pages) {
        this.pages = pages.isEmpty() ? List.of(List.of()) : pages;
        this.pageIndex = 0;
    }

    public static GuideListPager byItemCount(List<GuideLinkList.LinkItem> items, int itemsPerPage) {
        if (items.isEmpty()) {
            return new GuideListPager(List.of(List.of()));
        }
        int chunk = Math.max(1, itemsPerPage);
        List<List<GuideLinkList.LinkItem>> packed = new ArrayList<>();
        for (int start = 0; start < items.size(); start += chunk) {
            packed.add(new ArrayList<>(items.subList(start, Math.min(start + chunk, items.size()))));
        }
        return new GuideListPager(packed);
    }

    /** 高さベース分割は本テーマでは使わない */
    @Deprecated
    public static GuideListPager byHeight(List<GuideLinkList.LinkItem> items, Font font, int viewportHeight) {
        return byHeight(items, font, viewportHeight, 1);
    }

    public static GuideListPager byHeight(
            List<GuideLinkList.LinkItem> items,
            Font font,
            int viewportHeight,
            int listColumns
    ) {
        int packHeight = Math.max(1, viewportHeight - PACK_SAFETY);
        return new GuideListPager(packByHeight(items, font, packHeight, listColumns));
    }

    private static List<List<GuideLinkList.LinkItem>> packByHeight(
            List<GuideLinkList.LinkItem> items,
            Font font,
            int viewportHeight,
            int listColumns
    ) {
        if (items.isEmpty()) {
            return List.of(List.of());
        }

        List<List<GuideLinkList.LinkItem>> packed = new ArrayList<>();
        List<GuideLinkList.LinkItem> currentPage = new ArrayList<>();
        int usedHeight = 0;

        for (GuideLinkList.LinkItem item : items) {
            int rowHeight = GuideLinkList.itemRowHeight(font, item);
            int addition = currentPage.isEmpty()
                    ? rowHeight
                    : GuideLinkList.itemRowGap(currentPage.get(currentPage.size() - 1)) + rowHeight;
            if (listColumns > 1 && !item.isHeading() && !currentPage.isEmpty()) {
                GuideLinkList.LinkItem last = currentPage.get(currentPage.size() - 1);
                if (!last.isHeading()) {
                    int trailingLinks = countTrailingLinkItems(currentPage);
                    if (trailingLinks % listColumns != 0) {
                        addition = 0;
                    }
                }
            }

            if (!currentPage.isEmpty() && usedHeight + addition > viewportHeight) {
                packed.add(currentPage);
                currentPage = new ArrayList<>();
                usedHeight = 0;
                addition = rowHeight;
            }

            currentPage.add(item);
            usedHeight += addition;
        }

        if (!currentPage.isEmpty()) {
            packed.add(currentPage);
        }
        return packed;
    }

    private static int countTrailingLinkItems(List<GuideLinkList.LinkItem> page) {
        int count = 0;
        for (int i = page.size() - 1; i >= 0; i--) {
            GuideLinkList.LinkItem item = page.get(i);
            if (item.isHeading()) {
                break;
            }
            count++;
        }
        return count;
    }

    public void reset() {
        pageIndex = 0;
    }

    public int pageCount() {
        return pages.size();
    }

    public int pageIndex() {
        return pageIndex;
    }

    public boolean hasMultiplePages() {
        return pages.size() > 1;
    }

    public List<GuideLinkList.LinkItem> currentItems() {
        return pages.get(pageIndex);
    }

    public List<GuideLinkList.LinkItem> pageItems(int index) {
        return pages.get(index);
    }

    public boolean previousPage() {
        if (pageIndex <= 0) {
            return false;
        }
        pageIndex--;
        return true;
    }

    public boolean nextPage() {
        if (pageIndex >= pages.size() - 1) {
            return false;
        }
        pageIndex++;
        return true;
    }
}
