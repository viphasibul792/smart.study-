package com.sessiontracks.app.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Alarms do not survive a reboot, an app update or a clock change, so they are
 * rebuilt whenever the system reports one of those events.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(@NonNull Context context, @Nullable Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:
            case Intent.ACTION_MY_PACKAGE_REPLACED:
            case Intent.ACTION_TIME_CHANGED:
            case Intent.ACTION_TIMEZONE_CHANGED:
                Log.i(TAG, "Rescheduling reminders after " + action);
                ReminderScheduler.rescheduleAll(context.getApplicationContext());
                break;
            default:
                break;
        }
    }
}
