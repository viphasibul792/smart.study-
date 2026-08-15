package com.sessiontracks.app.data.model;

/** Dashboard header counters. */
public class OverallStats {

    public int sessionCount;
    public int subjectCount;
    public int chapterCount;
    public int totalLectures;
    public int completedLectures;

    public OverallStats() {
    }

    @androidx.room.Ignore
    public OverallStats(int sessionCount, int subjectCount, int chapterCount,
                        int totalLectures, int completedLectures) {
        this.sessionCount = sessionCount;
        this.subjectCount = subjectCount;
        this.chapterCount = chapterCount;
        this.totalLectures = totalLectures;
        this.completedLectures = completedLectures;
    }

    public int getPercent() {
        if (totalLectures <= 0) {
            return 0;
        }
        return Math.round((completedLectures * 100f) / totalLectures);
    }
}
