package com.sessiontracks.app.util;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a free-form block of pasted text into lecture links.
 *
 * <p>Two mapping strategies are supported:
 * <ul>
 *   <li><b>Sequential</b> — the Nth link found becomes Lecture N.</li>
 *   <li><b>Numbered</b> — if a line carries an explicit lecture number
 *       ("Lecture 5 - https://…", "05) https://…", "ক্লাস ৭ https://…"), the link is
 *       mapped to that number instead of its position.</li>
 * </ul>
 *
 * <p>The parser is deliberately tolerant: it accepts links separated by newlines,
 * spaces, commas or semicolons, ignores surrounding markdown/bullets, and strips
 * trailing punctuation that is not part of a URL.
 */
public final class LinkParser {

    /** Matches http/https URLs. Trailing punctuation is trimmed afterwards. */
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)\\b(?:https?://|www\\.)[^\\s<>\"'\\\\]+");

    /**
     * Detects a lecture number that appears before the URL on the same line.
     * Supports English and Bengali keywords plus bare "12." / "12)" prefixes.
     */
    private static final Pattern LECTURE_NUMBER_PATTERN = Pattern.compile(
            "(?i)(?:lecture|lec|class|video|part|ep(?:isode)?|ক্লাস|লেকচার|পর্ব)\\s*[.:#\\-]?\\s*(\\d{1,4})");

    private static final Pattern LEADING_NUMBER_PATTERN = Pattern.compile(
            "^\\s*[\\-*•]?\\s*(\\d{1,4})\\s*[).:\\-–]\\s+");

    /** Characters that commonly trail a pasted URL but are not part of it. */
    private static final String TRAILING_JUNK = ".,;:!?)]}'\"<>|";

    private LinkParser() {
    }

    /** One parsed link plus the lecture number it should map to (0 = unknown). */
    public static final class ParsedLink {
        public final String url;
        public final int lectureNumber;
        public final String label;

        public ParsedLink(String url, int lectureNumber, String label) {
            this.url = url;
            this.lectureNumber = lectureNumber;
            this.label = label;
        }
    }

    /** Extracts every URL in the order it appears, with any detected lecture number. */
    public static List<ParsedLink> parse(String rawText) {
        List<ParsedLink> results = new ArrayList<>();
        if (rawText == null || rawText.trim().isEmpty()) {
            return results;
        }
        // Split on newlines so a per-line lecture number can be attributed correctly.
        String[] lines = rawText.split("\\r?\\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            Matcher urlMatcher = URL_PATTERN.matcher(line);
            boolean firstOnLine = true;
            while (urlMatcher.find()) {
                String url = normalizeUrl(trimJunk(urlMatcher.group()));
                if (url.isEmpty()) {
                    continue;
                }
                // Only the first URL on a line inherits that line's lecture number;
                // extra URLs on the same line fall back to sequential placement.
                int number = firstOnLine ? detectLectureNumber(line, urlMatcher.start()) : 0;
                String label = firstOnLine ? extractLabel(line, urlMatcher.start()) : "";
                results.add(new ParsedLink(url, number, label));
                firstOnLine = false;
            }
        }
        return results;
    }

    /** Convenience: just the URLs, in order. */
    public static List<String> parseUrls(String rawText) {
        List<ParsedLink> parsed = parse(rawText);
        List<String> urls = new ArrayList<>(parsed.size());
        for (ParsedLink link : parsed) {
            urls.add(link.url);
        }
        return urls;
    }

    /**
     * Builds the final lecture-number to URL mapping.
     *
     * @param rawText     pasted text
     * @param useNumbers  when true, honour explicit "Lecture N" markers
     * @param maxLectures upper bound; links beyond it are dropped
     */
    public static Map<Integer, String> buildMapping(String rawText, boolean useNumbers, int maxLectures) {
        Map<Integer, String> mapping = new LinkedHashMap<>();
        List<ParsedLink> links = parse(rawText);
        if (links.isEmpty()) {
            return mapping;
        }

        if (useNumbers && hasExplicitNumbers(links)) {
            int fallbackCursor = 1;
            for (ParsedLink link : links) {
                int number = link.lectureNumber;
                if (number <= 0) {
                    // No marker on this line: take the next free slot.
                    while (mapping.containsKey(fallbackCursor)) {
                        fallbackCursor++;
                    }
                    number = fallbackCursor;
                }
                if (number >= 1 && number <= maxLectures) {
                    mapping.put(number, link.url);
                }
            }
        } else {
            int number = 1;
            for (ParsedLink link : links) {
                if (number > maxLectures) {
                    break;
                }
                mapping.put(number, link.url);
                number++;
            }
        }
        return mapping;
    }

    /** True when at least one line carried an explicit lecture number. */
    public static boolean hasExplicitNumbers(List<ParsedLink> links) {
        for (ParsedLink link : links) {
            if (link.lectureNumber > 0) {
                return true;
            }
        }
        return false;
    }

    /** Highest explicit lecture number found, or the link count when none exist. */
    public static int highestNumber(List<ParsedLink> links) {
        int max = 0;
        for (ParsedLink link : links) {
            max = Math.max(max, link.lectureNumber);
        }
        return Math.max(max, links.size());
    }

    /** Looks for "Lecture 5" style markers in the text preceding the URL. */
    private static int detectLectureNumber(String line, int urlStart) {
        String prefix = line.substring(0, Math.max(0, urlStart));
        String normalized = BengaliNumerals.toAscii(prefix);

        Matcher keyword = LECTURE_NUMBER_PATTERN.matcher(normalized);
        int found = 0;
        while (keyword.find()) {
            found = safeParse(keyword.group(1));
        }
        if (found > 0) {
            return found;
        }
        Matcher leading = LEADING_NUMBER_PATTERN.matcher(normalized);
        if (leading.find()) {
            return safeParse(leading.group(1));
        }
        // A prefix that is only digits, e.g. "07 https://…"
        String trimmed = normalized.trim();
        if (!trimmed.isEmpty() && trimmed.matches("\\d{1,4}")) {
            return safeParse(trimmed);
        }
        return 0;
    }

    /** Human-readable text before the URL, used for the preview list and titles. */
    private static String extractLabel(String line, int urlStart) {
        String prefix = line.substring(0, Math.max(0, urlStart)).trim();
        prefix = prefix.replaceAll("^[\\-*•\\d).:#\\s]+", "").trim();
        prefix = prefix.replaceAll("[\\-–:|]+$", "").trim();
        if (prefix.length() > 60) {
            prefix = prefix.substring(0, 60).trim();
        }
        return prefix;
    }

    private static int safeParse(String value) {
        if (value == null) {
            return 0;
        }
        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed < 1 || parsed > 9999) {
                return 0;
            }
            return (int) parsed;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Removes trailing punctuation and balances a trailing bracket if unmatched. */
    private static String trimJunk(String url) {
        String result = url.trim();
        while (!result.isEmpty() && TRAILING_JUNK.indexOf(result.charAt(result.length() - 1)) >= 0) {
            char last = result.charAt(result.length() - 1);
            if (last == ')' && countChar(result, '(') > countChar(result, ')')) {
                break;
            }
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static int countChar(String value, char target) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == target) {
                count++;
            }
        }
        return count;
    }

    /** Adds a scheme to bare "www." links so Intents can resolve them. */
    public static String normalizeUrl(String url) {
        String value = url.trim();
        if (value.isEmpty()) {
            return "";
        }
        String lower = value.toLowerCase(Locale.US);
        if (lower.startsWith("www.")) {
            return "https://" + value;
        }
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return "";
        }
        return value;
    }

    /** Basic validity check used by the manual link editor. */
    public static boolean isValidUrl(String url) {
        if (url == null) {
            return false;
        }
        String value = url.trim();
        if (value.isEmpty()) {
            return false;
        }
        String lower = value.toLowerCase(Locale.US);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return false;
        }
        // Must contain a host section beyond the scheme.
        int schemeEnd = lower.indexOf("//") + 2;
        return value.length() > schemeEnd && value.indexOf('.', schemeEnd) > schemeEnd;
    }

    /**
     * Extracts a YouTube video id from the common URL shapes so the app can hand a
     * precise intent to the YouTube app. Returns empty when the link is not YouTube.
     */
    public static String extractYouTubeId(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "";
        }
        String value = url.trim();
        Pattern pattern = Pattern.compile(
                "(?i)(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|embed/|shorts/|live/|v/)|youtu\\.be/)([A-Za-z0-9_-]{6,20})");
        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            String id = matcher.group(1);
            return id == null ? "" : id;
        }
        return "";
    }

    public static boolean isYouTube(String url) {
        return !extractYouTubeId(url).isEmpty();
    }
}
