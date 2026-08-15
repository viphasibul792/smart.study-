package com.sessiontracks.app.util;

/** Formats numbers honouring the Bengali-numeral preference. */
public final class Num {

    private final boolean bengali;

    public Num(boolean bengali) {
        this.bengali = bengali;
    }

    public String of(int value) {
        return bengali ? BengaliNumerals.toBengali(value) : String.valueOf(value);
    }

    public String text(String value) {
        return bengali ? BengaliNumerals.toBengali(value) : BengaliNumerals.toAscii(value);
    }

    public boolean isBengali() {
        return bengali;
    }
}
