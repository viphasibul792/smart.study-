package com.sessiontracks.app.model;

/** A study session block, e.g. "সেশন ১ : 7:00 PM - 8:00 PM". */
public class Session {
    public String id = "";
    public String name = "";
    /** Minutes from midnight (0..1439). */
    public int startMin;
    public int endMin;
    public int color;
    public int pos;
    public boolean reminder = true;

    /** Duration in minutes, handling sessions that wrap past midnight. */
    public int durationMinutes() {
        int d = endMin - startMin;
        if (d <= 0) d += 24 * 60;
        return d;
    }
}
