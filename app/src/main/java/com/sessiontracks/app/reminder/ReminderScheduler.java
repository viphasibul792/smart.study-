package com.sessiontracks.app.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.sessiontracks.app.data.entity.StudySession;
import com.sessiontracks.app.data.repository.PreferenceManager;
import com.sessiontracks.app.data.repository.StudyRepository;
import com.sessiontracks.app.util.AppExecutors;
import com.sessiontracks.app.util.TimeUtils;

import java.util.List;

/**
 * Schedules one repeating daily alarm per session. Exact alarms are used when the
 * user has granted the permission; otherwise the scheduler degrades to an inexact
 * alarm so reminders still arrive (just not to the minute).
 */
public final class ReminderScheduler {

    private static final String TAG = "ReminderScheduler";
    public static final String ACTION_SESSION_REMINDER = "com.sessiontracks.app.ACTION_SESSION_REMINDER";
    public static final String EXTRA_SESSION_ID = "extra_session_id";

    private ReminderScheduler() {
    }

    /** Cancels and re-creates alarms for every reminder-enabled session. */
    public static void rescheduleAll(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                StudyRepository repository = StudyRepository.getInstance(appContext);
                PreferenceManager preferences = PreferenceManager.getInstance(appContext);
                List<StudySession> sessions = repository.getReminderSessionsSync();

                for (StudySession session : sessions) {
                    cancel(appContext, session.getId());
                }
                if (!preferences.areRemindersEnabled()) {
                    Log.i(TAG, "Reminders disabled globally");
                    return;
                }
                int lead = preferences.getReminderLeadMinutes();
                for (StudySession session : sessions) {
                    schedule(appContext, session, lead);
                }
            } catch (Exception e) {
                Log.e(TAG, "rescheduleAll failed", e);
            }
        });
    }

    public static void schedule(@NonNull Context context, @NonNull StudySession session, int leadMinutes) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) {
            return;
        }
        long triggerAt = TimeUtils.nextOccurrenceMillis(session.getStartMinute(), leadMinutes);
        PendingIntent pendingIntent = buildPendingIntent(context, session.getId());

        try {
            if (canScheduleExact(manager)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
                } else {
                    manager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
                }
            } else {
                // Inexact fallback keeps the feature working without the special permission.
                manager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Exact alarm denied, using inexact", e);
            try {
                manager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            } catch (Exception ignored) {
                // Nothing more we can do.
            }
        } catch (Exception e) {
            Log.e(TAG, "schedule failed", e);
        }
    }

    public static void cancel(@NonNull Context context, @NonNull String sessionId) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) {
            return;
        }
        try {
            manager.cancel(buildPendingIntent(context, sessionId));
        } catch (Exception e) {
            Log.w(TAG, "cancel failed", e);
        }
    }

    public static boolean canScheduleExact(@NonNull Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return manager != null && canScheduleExact(manager);
    }

    private static boolean canScheduleExact(@NonNull AlarmManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return manager.canScheduleExactAlarms();
        }
        return true;
    }

    @NonNull
    private static PendingIntent buildPendingIntent(@NonNull Context context, @NonNull String sessionId) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_SESSION_REMINDER);
        intent.putExtra(EXTRA_SESSION_ID, sessionId);
        // A distinct data uri keeps one PendingIntent per session.
        intent.setData(android.net.Uri.parse("sessiontracks://reminder/" + sessionId));

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, sessionId.hashCode(), intent, flags);
    }
}
