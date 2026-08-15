package com.sessiontracks.app.data.model;

import androidx.annotation.NonNull;

import com.sessiontracks.app.data.entity.Subject;

/** A subject card plus its aggregated lecture progress. */
public class SubjectWithProgress {

    @NonNull
    private final Subject subject;
    @NonNull
    private final SubjectProgress progress;

    public SubjectWithProgress(@NonNull Subject subject, @NonNull SubjectProgress progress) {
        this.subject = subject;
        this.progress = progress;
    }

    @NonNull
    public Subject getSubject() {
        return subject;
    }

    @NonNull
    public SubjectProgress getProgress() {
        return progress;
    }

    public int getTotalLectures() {
        return progress.totalLectures;
    }

    public int getCompletedLectures() {
        return progress.completedLectures;
    }

    public int getChapterCount() {
        return progress.chapterCount;
    }

    public int getPercent() {
        return progress.getPercent();
    }
}
