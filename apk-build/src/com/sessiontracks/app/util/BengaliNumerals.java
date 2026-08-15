package com.sessiontracks.app.util;


/**
 * Bengali ↔ ASCII digit conversion. Users type "২০" for twenty lectures just as
 * often as "20", so every numeric input is normalized through here, and numbers
 * can be rendered back in Bengali when the user prefers.
 */
public final class BengaliNumerals {

    private static final char[] BENGALI_DIGITS = {'০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'};
    private static final char BENGALI_ZERO = '০';
    private static final char BENGALI_NINE = '৯';

    private BengaliNumerals() {
    }

    /** Converts any Bengali digits in the input to ASCII digits. */
    public static String toAscii(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= BENGALI_ZERO && c <= BENGALI_NINE) {
                builder.append((char) ('0' + (c - BENGALI_ZERO)));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    /** Converts ASCII digits to Bengali digits. */
    public static String toBengali(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= '0' && c <= '9') {
                builder.append(BENGALI_DIGITS[c - '0']);
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    public static String toBengali(int value) {
        return toBengali(String.valueOf(value));
    }

    /**
     * Parses a number that may use Bengali digits.
     *
     * @return the parsed value, or {@code fallback} when the text is not a number.
     */
    public static int parseInt(String input, int fallback) {
        if (input == null) {
            return fallback;
        }
        String normalized = toAscii(input).trim().replaceAll("[^0-9-]", "");
        if (normalized.isEmpty() || normalized.equals("-")) {
            return fallback;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Renders a number using the user's preferred digit set. */
    public static String format(int value, boolean useBengaliDigits) {
        return useBengaliDigits ? toBengali(value) : String.valueOf(value);
    }
}
