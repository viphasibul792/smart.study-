package com.sessiontracks.app.util;

import android.content.Context;
import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.Locale;

/** Converts between "minutes from midnight" and user-visible time strings. */
public final class TimeUtil {

    public static final int MINUTES_PER_DAY = 24 * 60;

    private TimeUtil() {
    }

    public static int toMinutes(int hour, int minute) {
        return ((hour % 24) * 60 + minute) % MINUTES_PER_DAY;
    }

    public static int hourOf(int m) {
        return ((m % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY / 60;
    }

    public static int minuteOf(int m) {
        return ((m % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY % 60;
    }

    /** Formats as "7:00 PM" or "19:00" following the device setting. */
    public static String format(Context ctx, int minutes) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hourOf(minutes));
        cal.set(Calendar.MINUTE, minuteOf(minutes));
        cal.set(Calendar.SECOND, 0);
        return DateFormat.getTimeFormat(ctx).format(cal.getTime());
    }

    public static String range(Context ctx, int start, int end) {
        return format(ctx, start) + " – " + format(ctx, end);
    }

    public static int nowMinutes() {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
    }

    /** True when "now" is inside the range, correctly handling midnight wrap. */
    public static boolean isNowWithin(int start, int end) {
        int now = nowMinutes();
        if (start == end) return false;
        if (start < end) return now >= start && now < end;
        return now >= start || now < end;
    }

    public static int minutesUntil(int target) {
        int d = target - nowMinutes();
        if (d < 0) d += MINUTES_PER_DAY;
        return d;
    }

    /** Epoch millis of the next occurrence of this time-of-day, always in the future. */
    public static long nextOccurrence(int minuteOfDay, int leadMinutes) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hourOf(minuteOfDay));
        cal.set(Calendar.MINUTE, minuteOf(minuteOfDay));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.MINUTE, -Math.max(0, leadMinutes));
        long now = System.currentTimeMillis();
        while (cal.getTimeInMillis() <= now) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return cal.getTimeInMillis();
    }

    public static String stamp(long millis) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US)
                .format(new java.util.Date(millis));
    }

    public static String dateTime(Context ctx, long millis) {
        if (millis <= 0) return "";
        java.util.Date d = new java.util.Date(millis);
        return DateFormat.getMediumDateFormat(ctx).format(d) + ", "
                + DateFormat.getTimeFormat(ctx).format(d);
    }
}
