package com.sessiontracks.app.model;

/** One lecture button: number, link, completion state and its own note. */
public class Lecture {
    public String id = "";
    public String chapterId = "";
    public int num;
    public String title = "";
    public String url = "";
    public boolean done;
    public long doneAt;
    public String notes = "";

    public boolean hasLink() {
        return url != null && url.trim().length() > 0;
    }

    public boolean hasNotes() {
        return notes != null && notes.trim().length() > 0;
    }
}
