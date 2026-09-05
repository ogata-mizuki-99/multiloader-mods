package com.ogatamizuki.guide.client.screen;

import net.minecraft.util.Mth;

/**
 * 羊皮紙パネル内リストのスクロール状態。
 */
public final class GuideScrollPanel {
    private int scrollOffset;

    public void reset() {
        scrollOffset = 0;
    }

    public int scrollOffset() {
        return scrollOffset;
    }

    public boolean scroll(double scrollY, int contentHeight, int viewportHeight) {
        if (contentHeight <= viewportHeight) {
            return false;
        }
        int maxScroll = contentHeight - viewportHeight;
        scrollOffset = Mth.clamp(scrollOffset - (int) (scrollY * GuideUi.SCROLL_STEP), 0, maxScroll);
        return true;
    }

    public boolean isScrollable(int contentHeight, int viewportHeight) {
        return contentHeight > viewportHeight;
    }
}
