package com.sessiontracks.app.data.model;

import androidx.annotation.NonNull;

/** Aggregated lecture counts for one chapter (populated by Room). */
public class ChapterProgress {

    @NonNull
    public String chapterId = "";
    public int totalLectures;
    public int completedLectures;

    public ChapterProgress() {
    }

    public ChapterProgress(@NonNull String chapterId, int totalLectures, int completedLectures) {
        this.chapterId = chapterId;
        this.totalLectures = totalLectures;
        this.completedLectures = completedLectures;
    }

    public int getPercent() {
        if (totalLectures <= 0) {
            return 0;
        }
        return Math.round((completedLectures * 100f) / totalLectures);
    }

    public boolean isComplete() {
        return totalLectures > 0 && completedLectures >= totalLectures;
    }
}
