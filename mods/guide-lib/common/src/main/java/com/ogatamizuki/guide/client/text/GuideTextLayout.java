package com.ogatamizuki.guide.client.text;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Guide 用テキスト折り返し。lang JSON の {@code \n} による明示改行と、
 * 日本語向け禁則処理を伴う自動折り返しを行う。
 */
public final class GuideTextLayout {
    private GuideTextLayout() {}

    public static List<FormattedCharSequence> split(Font font, Component text, int maxWidth) {
        List<FormattedCharSequence> result = new ArrayList<>();
        String plain = text.getString();
        int lineStart = 0;
        int length = plain.length();
        while (lineStart <= length) {
            int lineEnd = plain.indexOf('\n', lineStart);
            if (lineEnd < 0) {
                appendWrappedLines(font, plain.substring(lineStart), maxWidth, result);
                break;
            }
            appendWrappedLines(font, plain.substring(lineStart, lineEnd), maxWidth, result);
            result.add(FormattedCharSequence.EMPTY);
            lineStart = lineEnd + 1;
        }
        return result;
    }

    private static void appendWrappedLines(Font font, String line, int maxWidth, List<FormattedCharSequence> out) {
        if (line.isEmpty()) {
            return;
        }
        int start = 0;
        int length = line.length();
        while (start < length) {
            int end = findLineEnd(font, line, start, maxWidth);
            out.add(Component.literal(line.substring(start, end)).getVisualOrderText());
            start = end;
        }
    }

    private static int findLineEnd(Font font, String text, int start, int maxWidth) {
        int length = text.length();
        if (start >= length) {
            return start;
        }
        if (font.width(text.substring(start)) <= maxWidth) {
            return length;
        }

        int best = start;
        int index = start;
        while (index < length) {
            int next = text.offsetByCodePoints(index, 1);
            if (font.width(text.substring(start, next)) > maxWidth) {
                break;
            }
            best = next;
            index = next;
        }

        if (best == start) {
            return text.offsetByCodePoints(start, 1);
        }

        int preferred = findPreferredBreakIndex(text, start, best);
        return preferred > start ? preferred : best;
    }

    private static int findPreferredBreakIndex(String text, int start, int maxEnd) {
        for (int i = maxEnd; i > start; i--) {
            if (text.charAt(i - 1) == ' ' && isValidBreak(text, i)) {
                return i;
            }
        }
        for (int i = maxEnd; i > start; i--) {
            if (isBreakAfterChar(text, i - 1) && isValidBreak(text, i)) {
                return i;
            }
        }
        for (int i = maxEnd; i > start; i--) {
            if (isValidBreak(text, i)) {
                return i;
            }
        }
        return maxEnd;
    }

    private static boolean isValidBreak(String text, int breakIndex) {
        if (breakIndex <= 0) {
            return false;
        }
        if (breakIndex >= text.length()) {
            return true;
        }
        if (isLineStartForbidden(text, breakIndex)) {
            return false;
        }
        return !isLineEndForbidden(text, breakIndex - 1);
    }

    private static boolean isLineStartForbidden(String text, int index) {
        return isForbiddenAt(text, index, LINE_START_FORBIDDEN);
    }

    private static boolean isLineEndForbidden(String text, int index) {
        return isForbiddenAt(text, index, LINE_END_FORBIDDEN);
    }

    private static boolean isBreakAfterChar(String text, int index) {
        return isForbiddenAt(text, index, BREAK_AFTER);
    }

    private static boolean isForbiddenAt(String text, int index, int[] codePoints) {
        int cp = text.codePointAt(index);
        for (int forbidden : codePoints) {
            if (cp == forbidden) {
                return true;
            }
        }
        return false;
    }

    private static final int[] LINE_START_FORBIDDEN = {
            '。', '、', '.', ',', '!', '?', ')', '）', '」', '』', '】', '〉', '》',
            ':', ';', '・', '…', 'ー', '〜', '—', '–', '％', '%'
    };

    private static final int[] LINE_END_FORBIDDEN = {
            '(', '（', '「', '『', '【', '〈', '《', '［', '[', '｛', '{'
    };

    private static final int[] BREAK_AFTER = {
            '。', '、', '.', ',', '!', '?', ')', '）', '」', '』', '】', '〉', '》', ':', ';'
    };
}
