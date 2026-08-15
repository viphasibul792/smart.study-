package com.sessiontracks.app.model;

/** A subject card inside a session, with an optional custom time slot. */
public class Subject {
    public String id = "";
    public String sessionId = "";
    public String name = "";
    /** -1 means "inherit the session's time". */
    public int startMin = -1;
    public int endMin = -1;
    public int color;
    public int pos;

    public boolean hasCustomTime() {
        return startMin >= 0 && endMin >= 0;
    }
}
