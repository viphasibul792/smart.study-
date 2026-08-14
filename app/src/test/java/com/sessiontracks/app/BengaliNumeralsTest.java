package com.sessiontracks.app;

import static org.junit.Assert.assertEquals;

import com.sessiontracks.app.util.BengaliNumerals;

import org.junit.Test;

/** Unit tests for Bengali/ASCII digit handling. */
public class BengaliNumeralsTest {

    @Test
    public void toAscii_convertsBengaliDigits() {
        assertEquals("20", BengaliNumerals.toAscii("২০"));
        assertEquals("Chapter 02", BengaliNumerals.toAscii("Chapter ০২"));
        assertEquals("", BengaliNumerals.toAscii(null));
    }

    @Test
    public void toBengali_convertsAsciiDigits() {
        assertEquals("২০", BengaliNumerals.toBengali("20"));
        assertEquals("অধ্যায় ০২", BengaliNumerals.toBengali("অধ্যায় 02"));
    }

    @Test
    public void parseInt_acceptsBothNumeralSystems() {
        assertEquals(20, BengaliNumerals.parseInt("২০", -1));
        assertEquals(20, BengaliNumerals.parseInt("20", -1));
        assertEquals(20, BengaliNumerals.parseInt(" 20 ", -1));
        assertEquals(-1, BengaliNumerals.parseInt("abc", -1));
        assertEquals(-1, BengaliNumerals.parseInt("", -1));
        assertEquals(-1, BengaliNumerals.parseInt(null, -1));
    }

    @Test
    public void format_respectsPreference() {
        assertEquals("12", BengaliNumerals.format(12, false));
        assertEquals("১২", BengaliNumerals.format(12, true));
    }
}
