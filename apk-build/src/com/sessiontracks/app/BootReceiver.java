package com.sessiontracks.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Alarms do not survive reboot/update/clock changes, so rebuild them. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        Reminders.rescheduleAll(context.getApplicationContext());
    }
}
