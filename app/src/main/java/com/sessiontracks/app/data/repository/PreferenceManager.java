package com.sessiontracks.app.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * All user settings live in one SharedPreferences file so they survive process death
 * and are covered by Android auto-backup.
 */
public final class PreferenceManager {

    public static final String PREFS_NAME = "session_tracks_prefs";

    public static final String KEY_THEME = "pref_theme";
    public static final String KEY_LANGUAGE = "pref_language";
    public static final String KEY_BENGALI_NUMERALS = "pref_bengali_numerals";
    public static final String KEY_REMINDERS_ENABLED = "pref_reminders_enabled";
    public static final String KEY_REMINDER_LEAD = "pref_reminder_lead";
    public static final String KEY_OPEN_IN_APP = "pref_open_in_app";
    public static final String KEY_AUTO_COMPLETE = "pref_auto_complete";
    public static final String KEY_SEEDED = "pref_seeded";
    public static final String KEY_LAST_BACKUP = "pref_last_backup";

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    public static final String LANG_SYSTEM = "system";
    public static final String LANG_BENGALI = "bn";
    public static final String LANG_ENGLISH = "en";

    private static volatile PreferenceManager instance;

    private final SharedPreferences prefs;

    private PreferenceManager(@NonNull Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public static PreferenceManager getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (PreferenceManager.class) {
                if (instance == null) {
                    instance = new PreferenceManager(context);
                }
            }
        }
        return instance;
    }

    @NonNull
    public SharedPreferences raw() {
        return prefs;
    }

    // ----- Theme -----

    public int getThemeMode() {
        return prefs.getInt(KEY_THEME, THEME_SYSTEM);
    }

    public void setThemeMode(int mode) {
        prefs.edit().putInt(KEY_THEME, mode).apply();
    }

    /** Maps the stored preference to an AppCompatDelegate night mode constant. */
    public int getNightMode() {
        switch (getThemeMode()) {
            case THEME_LIGHT:
                return AppCompatDelegate.MODE_NIGHT_NO;
            case THEME_DARK:
                return AppCompatDelegate.MODE_NIGHT_YES;
            case THEME_SYSTEM:
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    // ----- Language -----

    @NonNull
    public String getLanguage() {
        return prefs.getString(KEY_LANGUAGE, LANG_SYSTEM);
    }

    public void setLanguage(@NonNull String language) {
        prefs.edit().putString(KEY_LANGUAGE, language).apply();
    }

    public boolean useBengaliNumerals() {
        return prefs.getBoolean(KEY_BENGALI_NUMERALS, false);
    }

    public void setUseBengaliNumerals(boolean value) {
        prefs.edit().putBoolean(KEY_BENGALI_NUMERALS, value).apply();
    }

    // ----- Reminders -----

    public boolean areRemindersEnabled() {
        return prefs.getBoolean(KEY_REMINDERS_ENABLED, true);
    }

    public void setRemindersEnabled(boolean value) {
        prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, value).apply();
    }

    /** Minutes before the session start that the notification fires. */
    public int getReminderLeadMinutes() {
        return prefs.getInt(KEY_REMINDER_LEAD, 5);
    }

    public void setReminderLeadMinutes(int minutes) {
        prefs.edit().putInt(KEY_REMINDER_LEAD, Math.max(0, minutes)).apply();
    }

    // ----- Playback -----

    public boolean openInApp() {
        return prefs.getBoolean(KEY_OPEN_IN_APP, false);
    }

    public void setOpenInApp(boolean value) {
        prefs.edit().putBoolean(KEY_OPEN_IN_APP, value).apply();
    }

    public boolean autoCompleteOnOpen() {
        return prefs.getBoolean(KEY_AUTO_COMPLETE, false);
    }

    public void setAutoCompleteOnOpen(boolean value) {
        prefs.edit().putBoolean(KEY_AUTO_COMPLETE, value).apply();
    }

    // ----- Bookkeeping -----

    public boolean isSeeded() {
        return prefs.getBoolean(KEY_SEEDED, false);
    }

    public void setSeeded(boolean value) {
        prefs.edit().putBoolean(KEY_SEEDED, value).apply();
    }

    public long getLastBackupTime() {
        return prefs.getLong(KEY_LAST_BACKUP, 0L);
    }

    public void setLastBackupTime(long millis) {
        prefs.edit().putLong(KEY_LAST_BACKUP, millis).apply();
    }

    public void registerListener(@NonNull SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterListener(@NonNull SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }
}
