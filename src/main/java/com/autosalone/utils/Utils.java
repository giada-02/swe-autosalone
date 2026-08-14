package com.autosalone.utils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

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

    public static LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateString); // formato YYYY-MM-DD
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format, must be YYYY-MM-DD");
        }
    }
}