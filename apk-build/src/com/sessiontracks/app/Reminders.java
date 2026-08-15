package com.sessiontracks.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.sessiontracks.app.db.DB;
import com.sessiontracks.app.model.Session;
import com.sessiontracks.app.util.Prefs;
import com.sessiontracks.app.util.TimeUtil;

import java.util.List;

/** Schedules and posts the daily session reminder notifications. */
public final class Reminders {

    private static final String TAG = "Reminders";
    public static final String CHANNEL = "session_reminders";
    public static final String ACTION = "com.sessiontracks.app.REMIND";
    public static final String EXTRA_ID = "session_id";

    private Reminders() {
    }

    public static void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, "সেশন রিমাইন্ডার",
                NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("পড়ার সেশন শুরুর আগে মনে করিয়ে দেয়");
        ch.enableVibration(true);
        nm.createNotificationChannel(ch);
    }

    /** Cancels and re-creates alarms for every reminder-enabled session. */
    public static void rescheduleAll(Context ctx) {
        try {
            Context app = ctx.getApplicationContext();
            DB db = DB.get(app);
            Prefs prefs = new Prefs(app);
            List<Session> all = db.sessions();
            for (Session s : all) cancel(app, s.id);
            if (!prefs.reminders()) return;
            for (Session s : all) {
                if (s.reminder) schedule(app, s, prefs.leadMinutes());
            }
        } catch (Exception e) {
            Log.e(TAG, "rescheduleAll", e);
        }
    }

    public static void schedule(Context ctx, Session s, int lead) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long at = TimeUtil.nextOccurrence(s.startMin, lead);
        PendingIntent pi = intentFor(ctx, s.id);
        try {
            boolean exact = true;
            if (Build.VERSION.SDK_INT >= 31) {
                // Compiled against an older SDK, so query the Android 12+ API reflectively.
                try {
                    Object r = AlarmManager.class.getMethod("canScheduleExactAlarms").invoke(am);
                    exact = (r instanceof Boolean) && ((Boolean) r).booleanValue();
                } catch (Throwable t) {
                    exact = false;
                }
            }
            if (exact && Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } else {
                // Inexact fallback keeps reminders working without the special grant.
                am.set(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (SecurityException se) {
            try {
                am.set(AlarmManager.RTC_WAKEUP, at, pi);
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            Log.e(TAG, "schedule", e);
        }
    }

    public static void cancel(Context ctx, String sessionId) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        try {
            am.cancel(intentFor(ctx, sessionId));
        } catch (Exception ignored) {
        }
    }

    private static PendingIntent intentFor(Context ctx, String sessionId) {
        Intent i = new Intent(ctx, ReminderReceiver.class);
        i.setAction(ACTION);
        i.putExtra(EXTRA_ID, sessionId);
        i.setData(Uri.parse("sessiontracks://reminder/" + sessionId));
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, sessionId.hashCode(), i, flags);
    }

    @SuppressWarnings("deprecation")
    public static void notifyNow(Context ctx, Session s, String subjects) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        createChannel(ctx);

        Intent open = new Intent(ctx, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(ctx, s.id.hashCode(), open, flags);

        String body = (subjects == null || subjects.trim().length() == 0)
                ? TimeUtil.range(ctx, s.startMin, s.endMin) + " — পড়তে বসার সময় হয়েছে"
                : "আজকের বিষয়: " + subjects;

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            b = new Notification.Builder(ctx, CHANNEL);
        } else {
            b = new Notification.Builder(ctx);
            b.setPriority(Notification.PRIORITY_HIGH);
        }
        b.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(s.name + " শুরু হচ্ছে")
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pi);
        try {
            nm.notify(s.id.hashCode(), b.build());
        } catch (Exception e) {
            Log.e(TAG, "notify", e);
        }
    }
}
