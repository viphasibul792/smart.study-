package com.sessiontracks.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.sessiontracks.app.util.LinkParser;

import org.junit.Test;

import java.util.List;
import java.util.Map;

/** Unit tests for the Bulk Link Importer parsing rules. */
public class LinkParserTest {

    @Test
    public void sequentialMapping_assignsLinksInOrder() {
        String input = "https://youtu.be/aaa\nhttps://youtu.be/bbb\nhttps://youtu.be/ccc";
        Map<Integer, String> mapping = LinkParser.buildMapping(input, true, 20);

        assertEquals(3, mapping.size());
        assertEquals("https://youtu.be/aaa", mapping.get(1));
        assertEquals("https://youtu.be/bbb", mapping.get(2));
        assertEquals("https://youtu.be/ccc", mapping.get(3));
    }

    @Test
    public void numberedMapping_respectsExplicitLectureNumbers() {
        String input = "Lecture 3 - https://a.com/3\nLecture 1 - https://a.com/1\nLecture 2 https://a.com/2";
        Map<Integer, String> mapping = LinkParser.buildMapping(input, true, 20);

        assertEquals("https://a.com/1", mapping.get(1));
        assertEquals("https://a.com/2", mapping.get(2));
        assertEquals("https://a.com/3", mapping.get(3));
    }

    @Test
    public void numberedMapping_supportsBengaliKeywordsAndDigits() {
        Map<Integer, String> mapping = LinkParser.buildMapping("ক্লাস ৫ https://a.com/five", true, 20);
        assertEquals("https://a.com/five", mapping.get(5));
    }

    @Test
    public void sequentialMode_ignoresExplicitNumbers() {
        String input = "Lecture 7 - https://a.com/x\nLecture 9 - https://a.com/y";
        Map<Integer, String> mapping = LinkParser.buildMapping(input, false, 20);

        assertEquals("https://a.com/x", mapping.get(1));
        assertEquals("https://a.com/y", mapping.get(2));
    }

    @Test
    public void parse_trimsTrailingPunctuation() {
        Map<Integer, String> mapping =
                LinkParser.buildMapping("1. https://a.com/one,\n2) https://a.com/two.", true, 20);
        assertEquals("https://a.com/one", mapping.get(1));
        assertEquals("https://a.com/two", mapping.get(2));
    }

    @Test
    public void parse_addsSchemeToBareWwwLinks() {
        Map<Integer, String> mapping = LinkParser.buildMapping("www.example.com/v1", true, 5);
        assertEquals("https://www.example.com/v1", mapping.get(1));
    }

    @Test
    public void buildMapping_clampsToMaximum() {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= 10; i++) {
            builder.append("https://a.com/").append(i).append('\n');
        }
        Map<Integer, String> mapping = LinkParser.buildMapping(builder.toString(), true, 5);
        assertEquals(5, mapping.size());
    }

    @Test
    public void buildMapping_fillsGapsForUnnumberedLines() {
        Map<Integer, String> mapping =
                LinkParser.buildMapping("Lecture 2 https://a.com/2\nhttps://a.com/gap", true, 10);
        assertEquals("https://a.com/2", mapping.get(2));
        assertEquals("https://a.com/gap", mapping.get(1));
    }

    @Test
    public void parse_handlesMessyRealWorldPaste() {
        String input = "অধ্যায় ০২: ভেক্টর\n"
                + "Lecture 1 — https://youtu.be/AAA111?t=30\n"
                + "Lecture 2 — https://www.youtube.com/watch?v=BBB222&list=PL1\n"
                + "লেকচার ৩: https://youtu.be/CCC333\n"
                + "• 4) https://youtu.be/DDD444\n"
                + "random text with no link\n"
                + "Lecture 5 https://youtube.com/shorts/EEE555";

        Map<Integer, String> mapping = LinkParser.buildMapping(input, true, 20);
        assertEquals(5, mapping.size());
        assertEquals("https://youtu.be/AAA111?t=30", mapping.get(1));
        assertEquals("https://www.youtube.com/watch?v=BBB222&list=PL1", mapping.get(2));
        assertEquals("https://youtu.be/CCC333", mapping.get(3));
        assertEquals("https://youtu.be/DDD444", mapping.get(4));
        assertEquals("https://youtube.com/shorts/EEE555", mapping.get(5));
    }

    @Test
    public void parse_handlesEmptyAndLinklessInput() {
        assertTrue(LinkParser.buildMapping("", true, 10).isEmpty());
        assertTrue(LinkParser.buildMapping(null, true, 10).isEmpty());
        assertTrue(LinkParser.buildMapping("just some notes", true, 10).isEmpty());
    }

    @Test
    public void parseUrls_returnsEveryUrlInOrder() {
        List<String> urls = LinkParser.parseUrls("a https://x.com/1 b https://x.com/2");
        assertEquals(2, urls.size());
        assertEquals("https://x.com/1", urls.get(0));
        assertEquals("https://x.com/2", urls.get(1));
    }

    @Test
    public void extractYouTubeId_supportsAllCommonFormats() {
        assertEquals("AAA111bbb", LinkParser.extractYouTubeId("https://youtu.be/AAA111bbb"));
        assertEquals("CCC333ddd",
                LinkParser.extractYouTubeId("https://www.youtube.com/watch?v=CCC333ddd&t=1"));
        assertEquals("EEE555fff", LinkParser.extractYouTubeId("https://youtube.com/shorts/EEE555fff"));
        assertEquals("ZZZ999yyy",
                LinkParser.extractYouTubeId("https://www.youtube.com/watch?list=PL1&v=ZZZ999yyy"));
        assertEquals("", LinkParser.extractYouTubeId("https://example.com/video"));
        assertFalse(LinkParser.isYouTube("https://example.com/video"));
        assertTrue(LinkParser.isYouTube("https://youtu.be/AAA111bbb"));
    }

    @Test
    public void isValidUrl_rejectsMalformedInput() {
        assertTrue(LinkParser.isValidUrl("https://youtu.be/abc"));
        assertTrue(LinkParser.isValidUrl("http://example.com/x"));
        assertFalse(LinkParser.isValidUrl(""));
        assertFalse(LinkParser.isValidUrl(null));
        assertFalse(LinkParser.isValidUrl("ftp://example.com"));
        assertFalse(LinkParser.isValidUrl("not a url"));
        assertFalse(LinkParser.isValidUrl("https://"));
    }
}
