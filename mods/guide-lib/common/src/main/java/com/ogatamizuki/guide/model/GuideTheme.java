package com.ogatamizuki.guide.model;

import com.ogatamizuki.guide.GuideLibCommon;
import net.minecraft.resources.Identifier;

public record GuideTheme(
        Identifier id,
        int colorTitle,
        int colorSubtitle,
        int colorBody,
        int colorMuted,
        int colorError,
        int colorAccent,
        int colorLink,
        int colorLinkHover,
        int colorPanelBg,
        int colorInnerBg,
        int colorDimOverlay,
        int colorSlotInner,
        FrameStyle frameStyle,
        Identifier openSound,
        Identifier pageTurnSound
) {
    public static final Identifier BOOK_ID = GuideLibCommon.id("book");
    public static final Identifier TABLET_ID = GuideLibCommon.id("tablet");

    public enum FrameStyle {
        MINECRAFT_BEVEL,
        PAPER,
        TABLET
    }

    public static GuideTheme bookBuiltin() {
        return new GuideTheme(
                BOOK_ID,
                0xFFB8860B,
                0xFF7A5C44,
                0xFF3D2914,
                0xFF8B7355,
                0xFFB33A3A,
                0xFFC9A227,
                0xFF1E5A8E,
                0xFFB8860B,
                0xFF5C4033,
                0xFFF2E0C4,
                0xB0282018,
                0xFFE0CFA8,
                FrameStyle.PAPER,
                GuideLibCommon.id("codex_open"),
                GuideLibCommon.id("codex_page")
        );
    }

    public static GuideTheme tabletBuiltin() {
        return new GuideTheme(
                TABLET_ID,
                0xFF00E5FF,
                0xFF80CFFF,
                0xFFB0E0FF,
                0xFF6699BB,
                0xFFFF6666,
                0xFF00AAFF,
                0xFF00CCFF,
                0xFFFFFFFF,
                0xFF0A1628,
                0xFF050D18,
                0xC0081018,
                0xFF020810,
                FrameStyle.TABLET,
                GuideLibCommon.id("tablet_open"),
                GuideLibCommon.id("tablet_beep")
        );
    }

    public GuideTheme withId(Identifier themeId) {
        return new GuideTheme(
                themeId,
                colorTitle,
                colorSubtitle,
                colorBody,
                colorMuted,
                colorError,
                colorAccent,
                colorLink,
                colorLinkHover,
                colorPanelBg,
                colorInnerBg,
                colorDimOverlay,
                colorSlotInner,
                frameStyle,
                openSound,
                pageTurnSound
        );
    }

    /** 目次一覧はタブレット UI のみスクロール。本風 UI はページ送り。 */
    public boolean usesListScroll() {
        return frameStyle == FrameStyle.TABLET;
    }

    public static final class Builder {
        private Identifier id = BOOK_ID;
        private int colorTitle;
        private int colorSubtitle;
        private int colorBody;
        private int colorMuted;
        private int colorError;
        private int colorAccent;
        private int colorLink;
        private int colorLinkHover;
        private int colorPanelBg;
        private int colorInnerBg;
        private int colorDimOverlay;
        private int colorSlotInner;
        private FrameStyle frameStyle = FrameStyle.MINECRAFT_BEVEL;
        private Identifier openSound;
        private Identifier pageTurnSound;

        private Builder() {}

        public static Builder from(GuideTheme theme) {
            Builder builder = new Builder();
            builder.id = theme.id;
            builder.colorTitle = theme.colorTitle;
            builder.colorSubtitle = theme.colorSubtitle;
            builder.colorBody = theme.colorBody;
            builder.colorMuted = theme.colorMuted;
            builder.colorError = theme.colorError;
            builder.colorAccent = theme.colorAccent;
            builder.colorLink = theme.colorLink;
            builder.colorLinkHover = theme.colorLinkHover;
            builder.colorPanelBg = theme.colorPanelBg;
            builder.colorInnerBg = theme.colorInnerBg;
            builder.colorDimOverlay = theme.colorDimOverlay;
            builder.colorSlotInner = theme.colorSlotInner;
            builder.frameStyle = theme.frameStyle;
            builder.openSound = theme.openSound;
            builder.pageTurnSound = theme.pageTurnSound;
            return builder;
        }

        public Builder id(Identifier themeId) {
            this.id = themeId;
            return this;
        }

        public Builder colorTitle(int value) { this.colorTitle = value; return this; }
        public Builder colorSubtitle(int value) { this.colorSubtitle = value; return this; }
        public Builder colorBody(int value) { this.colorBody = value; return this; }
        public Builder colorMuted(int value) { this.colorMuted = value; return this; }
        public Builder colorError(int value) { this.colorError = value; return this; }
        public Builder colorAccent(int value) { this.colorAccent = value; return this; }
        public Builder colorLink(int value) { this.colorLink = value; return this; }
        public Builder colorLinkHover(int value) { this.colorLinkHover = value; return this; }
        public Builder colorPanelBg(int value) { this.colorPanelBg = value; return this; }
        public Builder colorInnerBg(int value) { this.colorInnerBg = value; return this; }
        public Builder colorDimOverlay(int value) { this.colorDimOverlay = value; return this; }
        public Builder colorSlotInner(int value) { this.colorSlotInner = value; return this; }
        public Builder frameStyle(FrameStyle value) { this.frameStyle = value; return this; }
        public Builder openSound(Identifier value) { this.openSound = value; return this; }
        public Builder pageTurnSound(Identifier value) { this.pageTurnSound = value; return this; }

        public GuideTheme build() {
            return new GuideTheme(
                    id,
                    colorTitle,
                    colorSubtitle,
                    colorBody,
                    colorMuted,
                    colorError,
                    colorAccent,
                    colorLink,
                    colorLinkHover,
                    colorPanelBg,
                    colorInnerBg,
                    colorDimOverlay,
                    colorSlotInner,
                    frameStyle,
                    openSound,
                    pageTurnSound
            );
        }
    }
}
