package com.sessiontracks.app.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sessiontracks.app.data.entity.StudySession;
import com.sessiontracks.app.data.entity.Subject;
import com.sessiontracks.app.data.repository.PreferenceManager;
import com.sessiontracks.app.data.repository.StudyRepository;
import com.sessiontracks.app.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Fires when a session reminder alarm goes off: posts the notification and
 * re-arms tomorrow's alarm (alarms are one-shot so the daily repeat is explicit).
 */
public class ReminderReceiver extends BroadcastReceiver {

    private static final String TAG = "ReminderReceiver";

    @Override
    public void onReceive(@NonNull Context context, @Nullable Intent intent) {
        if (intent == null) {
            return;
        }
        String sessionId = intent.getStringExtra(ReminderScheduler.EXTRA_SESSION_ID);
        if (sessionId == null || sessionId.isEmpty()) {
            return;
        }
        Context appContext = context.getApplicationContext();
        // Database access must not happen on the main thread inside a receiver.
        final PendingResult pendingResult = goAsync();

        StudyRepository repository = StudyRepository.getInstance(appContext);
        repository.executors().diskIO().execute(() -> {
            try {
                PreferenceManager preferences = PreferenceManager.getInstance(appContext);
                StudySession session = repository.getSessionSync(sessionId);
                if (session == null || !session.isReminderEnabled() || !preferences.areRemindersEnabled()) {
                    return;
                }
                List<Subject> subjects = repository.getSubjectsSync(sessionId);
                List<String> names = new ArrayList<>(subjects.size());
                for (Subject subject : subjects) {
                    names.add(subject.getName());
                }
                String timeRange = TimeUtils.formatRange(appContext,
                        session.getStartMinute(), session.getEndMinute());

                NotificationHelper.createChannels(appContext);
                NotificationHelper.showSessionReminder(appContext, sessionId, session.getName(),
                        timeRange, android.text.TextUtils.join(", ", names));

                // Re-arm for the next day.
                ReminderScheduler.schedule(appContext, session, preferences.getReminderLeadMinutes());
            } catch (Exception e) {
                Log.e(TAG, "Reminder handling failed", e);
            } finally {
                pendingResult.finish();
            }
        });
    }
}
