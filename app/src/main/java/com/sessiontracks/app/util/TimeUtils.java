package com.sessiontracks.app.util;

import android.content.Context;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;

import com.sessiontracks.app.R;

import java.util.Calendar;
import java.util.Locale;

/**
 * Converts between "minutes from midnight" (how times are stored) and the
 * user-visible strings, honouring the device 12/24-hour setting.
 */
public final class TimeUtils {

    public static final int MINUTES_PER_DAY = 24 * 60;

    private TimeUtils() {
    }

    public static int toMinutes(int hourOfDay, int minute) {
        return ((hourOfDay % 24) * 60 + minute) % MINUTES_PER_DAY;
    }

    public static int hourOf(int minutesFromMidnight) {
        return Math.floorMod(minutesFromMidnight, MINUTES_PER_DAY) / 60;
    }

    public static int minuteOf(int minutesFromMidnight) {
        return Math.floorMod(minutesFromMidnight, MINUTES_PER_DAY) % 60;
    }

    /** Formats a stored minute value like "7:00 PM" or "19:00" depending on device settings. */
    @NonNull
    public static String formatTime(@NonNull Context context, int minutesFromMidnight) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOf(minutesFromMidnight));
        calendar.set(Calendar.MINUTE, minuteOf(minutesFromMidnight));
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        java.text.DateFormat formatter = DateFormat.getTimeFormat(context);
        return formatter.format(calendar.getTime());
    }

    /** "7:00 PM – 8:00 PM" */
    @NonNull
    public static String formatRange(@NonNull Context context, int startMinute, int endMinute) {
        return context.getString(R.string.session_time_range,
                formatTime(context, startMinute), formatTime(context, endMinute));
    }

    /** Human duration such as "1 hour 30 minutes", localized. */
    @NonNull
    public static String formatDuration(@NonNull Context context, int totalMinutes) {
        if (totalMinutes <= 0) {
            return context.getString(R.string.duration_minutes, 0);
        }
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        if (hours > 0 && minutes > 0) {
            return context.getString(R.string.duration_hours_minutes, hours, minutes);
        }
        if (hours > 0) {
            return context.getString(R.string.duration_hours, hours);
        }
        return context.getString(R.string.duration_minutes, minutes);
    }

    /** Current wall-clock time expressed as minutes from midnight. */
    public static int nowMinutes() {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
    }

    /** True when "now" falls inside the range, correctly handling midnight wrap. */
    public static boolean isNowWithin(int startMinute, int endMinute) {
        int now = nowMinutes();
        if (startMinute == endMinute) {
            return false;
        }
        if (startMinute < endMinute) {
            return now >= startMinute && now < endMinute;
        }
        // Wraps past midnight, e.g. 23:00 -> 00:00
        return now >= startMinute || now < endMinute;
    }

    /** Minutes until the next occurrence of the given time-of-day. */
    public static int minutesUntil(int targetMinute) {
        int now = nowMinutes();
        int delta = targetMinute - now;
        if (delta < 0) {
            delta += MINUTES_PER_DAY;
        }
        return delta;
    }

    /**
     * Epoch millis for the next occurrence of the given minute-of-day, offset by
     * {@code leadMinutes} earlier. Always strictly in the future.
     */
    public static long nextOccurrenceMillis(int minuteOfDay, int leadMinutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOf(minuteOfDay));
        calendar.set(Calendar.MINUTE, minuteOf(minuteOfDay));
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.MINUTE, -Math.max(0, leadMinutes));
        long now = System.currentTimeMillis();
        while (calendar.getTimeInMillis() <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        return calendar.getTimeInMillis();
    }

    /** Short date-time for "completed at" labels and backup file names. */
    @NonNull
    public static String formatDateTime(@NonNull Context context, long epochMillis) {
        if (epochMillis <= 0) {
            return "";
        }
        java.text.DateFormat date = DateFormat.getMediumDateFormat(context);
        java.text.DateFormat time = DateFormat.getTimeFormat(context);
        java.util.Date value = new java.util.Date(epochMillis);
        return date.format(value) + ", " + time.format(value);
    }

    /** Filesystem-safe timestamp, e.g. 2026-08-14-1930. */
    @NonNull
    public static String fileTimestamp(long epochMillis) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US)
                .format(new java.util.Date(epochMillis));
    }
}
