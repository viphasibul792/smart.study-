package com.sessiontracks.app.data.model;

import androidx.annotation.NonNull;

import com.sessiontracks.app.data.entity.Chapter;

/** A chapter row plus its aggregated lecture progress. */
public class ChapterWithProgress {

    @NonNull
    private final Chapter chapter;
    @NonNull
    private final ChapterProgress progress;

    public ChapterWithProgress(@NonNull Chapter chapter, @NonNull ChapterProgress progress) {
        this.chapter = chapter;
        this.progress = progress;
    }

    @NonNull
    public Chapter getChapter() {
        return chapter;
    }

    @NonNull
    public ChapterProgress getProgress() {
        return progress;
    }

    public int getPercent() {
        return progress.getPercent();
    }

    public int getTotalLectures() {
        return progress.totalLectures;
    }

    public int getCompletedLectures() {
        return progress.completedLectures;
    }
}
