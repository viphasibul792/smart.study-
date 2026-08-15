package com.sessiontracks.app.util;

import androidx.annotation.NonNull;

/**
 * Central place that turns integers into display strings, honouring the user's
 * Bengali-numeral preference. Every screen formats numbers through here so a
 * single toggle changes the whole app consistently.
 */
public final class NumberFormatter {

    private final boolean useBengaliDigits;

    public NumberFormatter(boolean useBengaliDigits) {
        this.useBengaliDigits = useBengaliDigits;
    }

    @NonNull
    public String format(int value) {
        return BengaliNumerals.format(value, useBengaliDigits);
    }

    @NonNull
    public String format(long value) {
        String plain = String.valueOf(value);
        return useBengaliDigits ? BengaliNumerals.toBengali(plain) : plain;
    }

    /** Converts any digits inside an arbitrary string (e.g. a chapter number). */
    @NonNull
    public String formatText(@NonNull String value) {
        return useBengaliDigits ? BengaliNumerals.toBengali(value) : BengaliNumerals.toAscii(value);
    }

    public boolean usesBengaliDigits() {
        return useBengaliDigits;
    }
}
