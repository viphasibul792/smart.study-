package com.sessiontracks.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.sessiontracks.app.data.model.ChapterProgress;
import com.sessiontracks.app.data.model.OverallStats;
import com.sessiontracks.app.data.model.SubjectProgress;

import java.util.List;

/**
 * Aggregate progress queries. Doing the counting in SQL keeps the UI thread free
 * and avoids loading every lecture row just to compute a percentage.
 */
@Dao
public interface ProgressDao {

    @Query("SELECT c.id AS chapterId, "
            + "COUNT(l.id) AS totalLectures, "
            + "COALESCE(SUM(CASE WHEN l.completed THEN 1 ELSE 0 END), 0) AS completedLectures "
            + "FROM chapters c LEFT JOIN lectures l ON l.chapter_id = c.id "
            + "GROUP BY c.id")
    LiveData<List<ChapterProgress>> observeChapterProgress();

    @Query("SELECT c.id AS chapterId, "
            + "COUNT(l.id) AS totalLectures, "
            + "COALESCE(SUM(CASE WHEN l.completed THEN 1 ELSE 0 END), 0) AS completedLectures "
            + "FROM chapters c LEFT JOIN lectures l ON l.chapter_id = c.id "
            + "WHERE c.subject_id = :subjectId GROUP BY c.id")
    LiveData<List<ChapterProgress>> observeChapterProgressForSubject(String subjectId);

    @Query("SELECT c.id AS chapterId, "
            + "COUNT(l.id) AS totalLectures, "
            + "COALESCE(SUM(CASE WHEN l.completed THEN 1 ELSE 0 END), 0) AS completedLectures "
            + "FROM chapters c LEFT JOIN lectures l ON l.chapter_id = c.id "
            + "WHERE c.id = :chapterId GROUP BY c.id")
    LiveData<ChapterProgress> observeSingleChapterProgress(String chapterId);

    @Query("SELECT s.id AS subjectId, "
            + "COUNT(l.id) AS totalLectures, "
            + "COALESCE(SUM(CASE WHEN l.completed THEN 1 ELSE 0 END), 0) AS completedLectures, "
            + "(SELECT COUNT(*) FROM chapters c2 WHERE c2.subject_id = s.id) AS chapterCount "
            + "FROM subjects s "
            + "LEFT JOIN chapters c ON c.subject_id = s.id "
            + "LEFT JOIN lectures l ON l.chapter_id = c.id "
            + "GROUP BY s.id")
    LiveData<List<SubjectProgress>> observeSubjectProgress();

    @Query("SELECT "
            + "(SELECT COUNT(*) FROM sessions) AS sessionCount, "
            + "(SELECT COUNT(*) FROM subjects) AS subjectCount, "
            + "(SELECT COUNT(*) FROM chapters) AS chapterCount, "
            + "(SELECT COUNT(*) FROM lectures) AS totalLectures, "
            + "(SELECT COUNT(*) FROM lectures WHERE completed = 1) AS completedLectures")
    LiveData<OverallStats> observeOverallStats();
}
