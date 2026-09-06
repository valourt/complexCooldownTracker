package com.tomcraft.cooldowntracker.config;

/**
 * A hand-written whole-word substring scan, used in place of the regex
 * engine for the common case of "does this literal name appear in this
 * text". Regex has real per-call overhead even pre-compiled; when the same
 * short literal text gets checked against hundreds of tracked items,
 * hundreds of times a second (chat dumps, inventory scans), that overhead
 * adds up to actual measurable lag. A plain indexOf-based scan does not
 * carry that cost.
 */
public class TextMatch {

    /**
     * @param haystackLower already-lowercased text to search within
     * @param needleLower   already-lowercased, already-normalized text to find
     */
    public static boolean containsWholeWord(String haystackLower, String needleLower) {
        if (needleLower == null || needleLower.isEmpty() || haystackLower == null) return false;
        int idx = haystackLower.indexOf(needleLower);
        while (idx != -1) {
            boolean leftOk = idx == 0 || !Character.isLetterOrDigit(haystackLower.charAt(idx - 1));
            int endIdx = idx + needleLower.length();
            boolean rightOk = endIdx == haystackLower.length() || !Character.isLetterOrDigit(haystackLower.charAt(endIdx));
            if (leftOk && rightOk) return true;
            idx = haystackLower.indexOf(needleLower, idx + 1);
        }
        return false;
    }

    /**
     * Lowercases and strips anything that isn't a letter/digit/space (this
     * removes icon glyphs some servers prefix item names with), collapsing
     * whitespace - so matching is by "contains" on cleaned-up text rather
     * than a fragile exact match.
     */
    public static String normalize(String s) {
        if (s == null) return "";
        String cleaned = s.replaceAll("[^\\p{L}\\p{Nd}\\s]", "");
        cleaned = cleaned.replaceAll("\\s+", " ").trim().toLowerCase();
        return cleaned;
    }
}
