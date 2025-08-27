// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.chat;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code LineWrapper} class is used to wrap lines of text based on a rough
 * visual width. It attempts to break on whitespace when possible but will also
 * split long strings with no spaces (e.g. Chinese) so that text fits within the
 * chat bubble.
 */
public class LineWrapper {

    public static List<String> wrapLines(String text, int maxWidth) {
        List<String> wrappedLines = new ArrayList<>();
        if (text == null || text.isEmpty()) return wrappedLines;

        int lineStart = 0;      // index where the current line starts
        int lastSpace = -1;     // index of the last seen whitespace character
        int lineWidth = 0;      // visual width of the current line

        for (int i = 0; i < text.length();) {
            int codePoint = text.codePointAt(i);
            int charWidth = isWide(codePoint) ? 2 : 1;

            if (Character.isWhitespace(codePoint)) {
                lastSpace = i;
            }

            if (lineWidth + charWidth > maxWidth) {
                int breakPos;
                if (lastSpace >= lineStart) {
                    breakPos = lastSpace;
                } else {
                    breakPos = i;
                }

                wrappedLines.add(text.substring(lineStart, breakPos).trim());
                lineStart = breakPos;
                lineWidth = 0;
                lastSpace = -1;

                // Skip leading whitespace on the next line
                if (lineStart < text.length() && Character.isWhitespace(text.codePointAt(lineStart))) {
                    lineStart += Character.charCount(text.codePointAt(lineStart));
                }
                i = lineStart;
                continue;
            }

            lineWidth += charWidth;
            i += Character.charCount(codePoint);
        }

        if (lineStart < text.length()) {
            wrappedLines.add(text.substring(lineStart).trim());
        }

        return wrappedLines;
    }

    private static boolean isWide(int codePoint) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA
                || block == Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS
                || block == Character.UnicodeBlock.HANGUL_SYLLABLES
                || block == Character.UnicodeBlock.HANGUL_JAMO
                || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }
}
