package com.sessiontracks.app.data.model;

import androidx.annotation.NonNull;

/** Aggregated lecture counts for one subject (populated by Room). */
public class SubjectProgress {

    @NonNull
    public String subjectId = "";
    public int totalLectures;
    public int completedLectures;
    public int chapterCount;

    public SubjectProgress() {
    }

    public SubjectProgress(@NonNull String subjectId, int totalLectures, int completedLectures, int chapterCount) {
        this.subjectId = subjectId;
        this.totalLectures = totalLectures;
        this.completedLectures = completedLectures;
        this.chapterCount = chapterCount;
    }

    public int getPercent() {
        if (totalLectures <= 0) {
            return 0;
        }
        return Math.round((completedLectures * 100f) / totalLectures);
    }
}
