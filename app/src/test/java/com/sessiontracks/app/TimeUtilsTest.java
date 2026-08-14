package com.sessiontracks.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.sessiontracks.app.util.TimeUtils;

import org.junit.Test;

/** Unit tests for minute-of-day maths (no Android context required). */
public class TimeUtilsTest {

    @Test
    public void toMinutes_convertsClockTime() {
        assertEquals(19 * 60, TimeUtils.toMinutes(19, 0));
        assertEquals(19 * 60 + 30, TimeUtils.toMinutes(19, 30));
        assertEquals(0, TimeUtils.toMinutes(24, 0));
    }

    @Test
    public void hourAndMinute_roundTrip() {
        int value = TimeUtils.toMinutes(21, 45);
        assertEquals(21, TimeUtils.hourOf(value));
        assertEquals(45, TimeUtils.minuteOf(value));
    }

    @Test
    public void isNowWithin_handlesEqualBoundsAsEmptyRange() {
        assertFalse(TimeUtils.isNowWithin(600, 600));
    }

    @Test
    public void isNowWithin_coversFullDayRange() {
        // A range spanning the whole day must always contain "now".
        assertTrue(TimeUtils.isNowWithin(0, TimeUtils.MINUTES_PER_DAY - 1)
                || TimeUtils.nowMinutes() == TimeUtils.MINUTES_PER_DAY - 1);
    }

    @Test
    public void minutesUntil_isAlwaysWithinOneDay() {
        for (int minute = 0; minute < TimeUtils.MINUTES_PER_DAY; minute += 137) {
            int delta = TimeUtils.minutesUntil(minute);
            assertTrue(delta >= 0 && delta < TimeUtils.MINUTES_PER_DAY);
        }
    }

    @Test
    public void nextOccurrenceMillis_isAlwaysInTheFuture() {
        long now = System.currentTimeMillis();
        assertTrue(TimeUtils.nextOccurrenceMillis(0, 0) > now);
        assertTrue(TimeUtils.nextOccurrenceMillis(TimeUtils.nowMinutes(), 0) > now);
        assertTrue(TimeUtils.nextOccurrenceMillis(TimeUtils.nowMinutes(), 30) > now);
    }
}
