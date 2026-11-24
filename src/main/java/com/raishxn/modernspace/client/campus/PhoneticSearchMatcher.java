package com.raishxn.modernspace.client.campus;

import java.text.Normalizer;
import java.util.Locale;

/** Accent/case-insensitive substring matcher (the original used pinyin; GTO port uses diacritic folding). */
public final class PhoneticSearchMatcher {
    private final String query;
    private PhoneticSearchMatcher(String query) { this.query = query; }
    public static PhoneticSearchMatcher compile(String query) { return new PhoneticSearchMatcher(fold(query)); }
    public boolean matches(String text) { return query.isEmpty() || fold(text).contains(query); }
    public boolean matches(SearchText text) { return query.isEmpty() || text.folded.contains(query); }
    public static SearchText prepare(String text) { return new SearchText(fold(text)); }
    public static void clearCaches() {}
    static String fold(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim();
    }
    public static final class SearchText {
        private final String folded;
        private SearchText(String folded) { this.folded = folded; }
    }
}
