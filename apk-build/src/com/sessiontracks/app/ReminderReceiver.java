package com.sessiontracks.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sessiontracks.app.db.DB;
import com.sessiontracks.app.model.Session;
import com.sessiontracks.app.model.Subject;
import com.sessiontracks.app.util.Prefs;

import java.util.List;

/** Posts a session reminder and re-arms tomorrow's alarm. */
public class ReminderReceiver extends BroadcastReceiver {

    private static final String TAG = "ReminderReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent == null) return;
        final String id = intent.getStringExtra(Reminders.EXTRA_ID);
        if (id == null || id.length() == 0) return;

        final Context app = context.getApplicationContext();
        final PendingResult result = goAsync();
        // Database work must not run on the main thread inside a receiver.
        new Thread(new Runnable() {
            public void run() {
                try {
                    DB db = DB.get(app);
                    Prefs prefs = new Prefs(app);
                    Session s = db.session(id);
                    if (s == null || !s.reminder || !prefs.reminders()) return;

                    List<Subject> subs = db.subjects(id);
                    StringBuilder sb = new StringBuilder();
                    for (Subject x : subs) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(x.name);
                    }
                    Reminders.notifyNow(app, s, sb.toString());
                    Reminders.schedule(app, s, prefs.leadMinutes());
                } catch (Exception e) {
                    Log.e(TAG, "onReceive", e);
                } finally {
                    result.finish();
                }
            }
        }).start();
    }
}
