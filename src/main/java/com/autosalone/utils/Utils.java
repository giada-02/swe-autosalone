package com.autosalone.utils;

public class Utils {

    private Utils() {
    }

    public static String sanitizeLikeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed.replace("%", "\\%").replace("_", "\\_");
    }

    public static String sanitizeText(String text) {
        return (text == null || text.isBlank()) ? null : text.trim();
    }
}