package com.sessiontracks.app.reminder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.sessiontracks.app.R;
import com.sessiontracks.app.ui.main.MainActivity;

/** Creates the reminder channel and posts session notifications. */
public final class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    public static final String CHANNEL_REMINDERS = "session_reminders";

    private NotificationHelper() {
    }

    /** Safe to call repeatedly; creating an existing channel is a no-op. */
    public static void createChannels(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.notif_channel_reminder),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.notif_channel_reminder_desc));
        channel.enableVibration(true);
        channel.setShowBadge(true);
        manager.createNotificationChannel(channel);
    }

    /** True when the app is allowed to post notifications (Android 13+ runtime permission). */
    public static boolean canPostNotifications(@NonNull Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public static void showSessionReminder(@NonNull Context context, @NonNull String sessionId,
                                           @NonNull String sessionName, @NonNull String timeRange,
                                           @Nullable String subjectNames) {
        if (!canPostNotifications(context)) {
            Log.i(TAG, "Notifications disabled; skipping reminder");
            return;
        }

        Intent contentIntent = new Intent(context, MainActivity.class);
        contentIntent.setAction(Intent.ACTION_VIEW);
        contentIntent.setData(Uri.parse("sessiontracks://dashboard?session=" + Uri.encode(sessionId)));
        contentIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, sessionId.hashCode(), contentIntent, flags);

        String body = subjectNames == null || subjectNames.trim().isEmpty()
                ? context.getString(R.string.notif_reminder_body, timeRange)
                : context.getString(R.string.notif_reminder_body_subjects, subjectNames);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_notification_small)
                .setColor(ContextCompat.getColor(context, R.color.brand_600))
                .setContentTitle(context.getString(R.string.notif_reminder_title, sessionName))
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context).notify(sessionId.hashCode(), builder.build());
        } catch (SecurityException e) {
            // Permission revoked between the check and the post.
            Log.w(TAG, "Notification blocked", e);
        }
    }
}
