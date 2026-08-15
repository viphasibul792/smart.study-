package com.sessiontracks.app;

import android.app.Application;
import android.content.res.Configuration;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.sessiontracks.app.data.repository.PreferenceManager;
import com.sessiontracks.app.data.repository.StudyRepository;
import com.sessiontracks.app.reminder.NotificationHelper;
import com.sessiontracks.app.reminder.ReminderScheduler;

/**
 * Application entry point: applies the saved theme/locale before any Activity is
 * created, seeds the default sessions on first launch and re-arms reminders.
 */
public class SessionTracksApp extends Application {

    private static final String TAG = "SessionTracksApp";

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            enableStrictMode();
        }

        PreferenceManager preferences = PreferenceManager.getInstance(this);
        AppCompatDelegate.setDefaultNightMode(preferences.getNightMode());
        applyLanguage(preferences.getLanguage());

        NotificationHelper.createChannels(this);

        // First run: create the three default study sessions.
        StudyRepository repository = StudyRepository.getInstance(this);
        repository.seedDefaultsIfEmpty((success, seeded, error) -> {
            if (success && Boolean.TRUE.equals(seeded)) {
                preferences.setSeeded(true);
                Log.i(TAG, "Default sessions seeded");
            }
            ReminderScheduler.rescheduleAll(this);
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /** Applies the in-app language selection using AndroidX per-app locales. */
    public static void applyLanguage(@NonNull String language) {
        LocaleListCompat locales;
        if (PreferenceManager.LANG_SYSTEM.equals(language)) {
            locales = LocaleListCompat.getEmptyLocaleList();
        } else {
            locales = LocaleListCompat.forLanguageTags(language);
        }
        AppCompatDelegate.setApplicationLocales(locales);
    }

    private void enableStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build());
    }
}
