package Util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Functions {
    public static List<String> extractWords(String text) {
        List<String> words = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\b\\p{L}+'?\\p{L}*\\b").matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            words.add(matcher.group());
        }
        return words;
    }

    public static boolean isValidTimeFormat(String dateTime) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy, HH:mm");
            LocalDateTime.parse(dateTime, formatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static LocalDateTime parseDateTime(String dateTimeStr) {
        // Remove any whitespace and split by comma
        String[] parts = dateTimeStr.trim().split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid date-time format. Expected format: DD.MM.YY, HH:mm");
        }

        // Combine date and time parts with proper formatting
        String dateTime = parts[0].trim() + " " + parts[1].trim();

        // Create formatter for the specific pattern
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");

        try {
            // Parse the string to LocalDateTime
            return LocalDateTime.parse(dateTime, formatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Failed to parse date-time: " + dateTimeStr, e);
        }
    }
}
