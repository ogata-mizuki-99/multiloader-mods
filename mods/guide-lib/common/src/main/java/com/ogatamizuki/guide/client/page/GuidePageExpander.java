package com.ogatamizuki.guide.client.page;

import com.google.gson.JsonObject;
import com.ogatamizuki.guide.client.GuideLibClient;
import com.ogatamizuki.guide.client.screen.GuideUi;
import com.ogatamizuki.guide.client.text.GuideTextLayout;
import com.ogatamizuki.guide.model.GuidePage;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class GuidePageExpander {
    private static final int TEXT_PADDING = 16;
    private static final int LINE_SPACING = 2;

    private static final Set<String> KNOWN_TYPES = Set.of("text", "crafting", "spotlight", "image");

    private GuidePageExpander() {}

    public static List<GuidePage> expand(List<GuidePage> pages, Font font, int contentWidth, int contentHeight) {
        List<GuidePage> expanded = new ArrayList<>();
        for (GuidePage page : pages) {
            expanded.addAll(expandPage(page, font, contentWidth, contentHeight));
        }
        return expanded;
    }

    private static List<GuidePage> expandPage(GuidePage page, Font font, int contentWidth, int contentHeight) {
        return switch (page.type()) {
            case "text" -> expandText(page, font, contentWidth, contentHeight);
            case "spotlight" -> expandSpotlight(page, font, contentWidth, contentHeight);
            default -> List.of(page);
        };
    }

    private static List<GuidePage> expandText(GuidePage page, Font font, int contentWidth, int contentHeight) {
        if (!page.data().has("text")) {
            return List.of(page);
        }
        Component text = GuideLibClient.translateOrLiteral(page.data().get("text").getAsString());
        int wrapWidth = contentWidth - TEXT_PADDING;
        int maxLines = maxLinesForHeight(font, contentHeight, GuideUi.PAGE_TEXT_TOP, GuideUi.PAGE_TEXT_BOTTOM);
        return splitLines(page, font, text, wrapWidth, maxLines);
    }

    private static List<GuidePage> expandSpotlight(GuidePage page, Font font, int contentWidth, int contentHeight) {
        if (!page.data().has("text")) {
            return List.of(page);
        }
        Component text = GuideLibClient.translateOrLiteral(page.data().get("text").getAsString());
        int wrapWidth = contentWidth - TEXT_PADDING;
        int spotlightMaxLines = maxLinesForHeight(
                font,
                contentHeight,
                GuideUi.SPOTLIGHT_HEADER_HEIGHT,
                GuideUi.PAGE_TEXT_BOTTOM
        );
        int textMaxLines = maxLinesForHeight(
                font,
                contentHeight,
                GuideUi.PAGE_TEXT_TOP,
                GuideUi.PAGE_TEXT_BOTTOM
        );
        List<FormattedCharSequence> lines = GuideTextLayout.split(font, text, wrapWidth);
        if (lines.size() <= spotlightMaxLines) {
            return List.of(page);
        }

        List<GuidePage> result = new ArrayList<>();
        JsonObject firstPageData = page.data().deepCopy();
        firstPageData.addProperty("text", "literal:" + joinLines(lines, 0, spotlightMaxLines));
        result.add(new GuidePage("spotlight", firstPageData));

        for (int start = spotlightMaxLines; start < lines.size(); start += textMaxLines) {
            int end = Math.min(start + textMaxLines, lines.size());
            JsonObject chunkData = new JsonObject();
            chunkData.addProperty("type", "text");
            chunkData.addProperty("text", "literal:" + joinLines(lines, start, end));
            result.add(new GuidePage("text", chunkData));
        }
        return result;
    }

    private static String joinLines(List<FormattedCharSequence> lines, int start, int end) {
        StringBuilder chunkText = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (i > start) {
                chunkText.append('\n');
            }
            chunkText.append(linePlainText(lines.get(i)));
        }
        return chunkText.toString();
    }

    private static int maxLinesForHeight(Font font, int contentHeight, int topPadding, int bottomPadding) {
        int available = contentHeight - topPadding - bottomPadding;
        if (available <= 0) {
            return 1;
        }
        return Math.max(1, (available + LINE_SPACING) / (font.lineHeight + LINE_SPACING));
    }

    private static List<GuidePage> splitLines(
            GuidePage sourcePage,
            Font font,
            Component text,
            int wrapWidth,
            int maxLines
    ) {
        List<FormattedCharSequence> lines = GuideTextLayout.split(font, text, wrapWidth);
        if (lines.size() <= maxLines) {
            return List.of(sourcePage);
        }

        List<GuidePage> chunks = new ArrayList<>();
        for (int start = 0; start < lines.size(); start += maxLines) {
            int end = Math.min(start + maxLines, lines.size());
            JsonObject chunkData = new JsonObject();
            chunkData.addProperty("type", "text");
            chunkData.addProperty("text", "literal:" + joinLines(lines, start, end));
            chunks.add(new GuidePage("text", chunkData));
        }
        return chunks;
    }

    private static String linePlainText(FormattedCharSequence line) {
        StringBuilder builder = new StringBuilder();
        line.accept((index, style, codePoint) -> {
            builder.appendCodePoint(codePoint);
            return true;
        });
        return builder.toString();
    }

    public static boolean isKnownType(String type) {
        return KNOWN_TYPES.contains(type);
    }
}
