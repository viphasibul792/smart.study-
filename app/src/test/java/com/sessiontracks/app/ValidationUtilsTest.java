package com.sessiontracks.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.sessiontracks.app.util.ValidationUtils;

import org.junit.Test;

/** Unit tests for input clamping that does not require Android context. */
public class ValidationUtilsTest {

    @Test
    public void clamp_truncatesLongText() {
        assertEquals("abc", ValidationUtils.clamp("abcdef", 3));
        assertEquals("abc", ValidationUtils.clamp("abc", 10));
        assertEquals("", ValidationUtils.clamp(null, 10));
    }

    @Test
    public void clamp_neverThrowsOnBoundaries() {
        assertTrue(ValidationUtils.clamp("abcdef", 0).isEmpty());
    }
}
