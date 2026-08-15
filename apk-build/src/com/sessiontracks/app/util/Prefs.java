package com.sessiontracks.app.util;

import android.content.Context;
import android.content.SharedPreferences;

/** All user settings in one SharedPreferences file. */
public final class Prefs {

    private static final String FILE = "session_tracks_prefs";
    private final SharedPreferences p;

    public Prefs(Context ctx) {
        p = ctx.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public boolean dark() {
        return p.getBoolean("dark", false);
    }

    public void setDark(boolean v) {
        p.edit().putBoolean("dark", v).apply();
    }

    public boolean bengaliDigits() {
        return p.getBoolean("bn_digits", true);
    }

    public void setBengaliDigits(boolean v) {
        p.edit().putBoolean("bn_digits", v).apply();
    }

    public boolean reminders() {
        return p.getBoolean("reminders", true);
    }

    public void setReminders(boolean v) {
        p.edit().putBoolean("reminders", v).apply();
    }

    public boolean openInApp() {
        return p.getBoolean("in_app", false);
    }

    public void setOpenInApp(boolean v) {
        p.edit().putBoolean("in_app", v).apply();
    }

    public boolean autoDone() {
        return p.getBoolean("auto_done", false);
    }

    public void setAutoDone(boolean v) {
        p.edit().putBoolean("auto_done", v).apply();
    }

    public int leadMinutes() {
        return p.getInt("lead", 5);
    }

    public void setLeadMinutes(int v) {
        p.edit().putInt("lead", Math.max(0, v)).apply();
    }

    public long lastBackup() {
        return p.getLong("last_backup", 0L);
    }

    public void setLastBackup(long v) {
        p.edit().putLong("last_backup", v).apply();
    }
}
