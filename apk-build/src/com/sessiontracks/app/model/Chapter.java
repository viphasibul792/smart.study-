package com.sessiontracks.app.model;

/** A chapter inside a subject, e.g. "অধ্যায় ০২: ভেক্টর" with 20 lectures. */
public class Chapter {
    public String id = "";
    public String subjectId = "";
    public String num = "";
    public String name = "";
    public int total;
    public String notes = "";
    public int pos;

    public boolean hasNotes() {
        return notes != null && notes.trim().length() > 0;
    }
}
