package com.sessiontracks.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.sessiontracks.app.data.entity.Lecture;

import java.util.List;

@Dao
public interface LectureDao {

    @Query("SELECT * FROM lectures WHERE chapter_id = :chapterId ORDER BY number ASC")
    LiveData<List<Lecture>> observeByChapter(String chapterId);

    @Query("SELECT * FROM lectures WHERE chapter_id = :chapterId ORDER BY number ASC")
    List<Lecture> getByChapterSync(String chapterId);

    @Query("SELECT * FROM lectures ORDER BY chapter_id ASC, number ASC")
    List<Lecture> getAllSync();

    @Query("SELECT * FROM lectures WHERE id = :id LIMIT 1")
    Lecture getByIdSync(String id);

    @Query("SELECT * FROM lectures WHERE chapter_id = :chapterId AND number = :number LIMIT 1")
    Lecture getByNumberSync(String chapterId, int number);

    @Query("SELECT COUNT(*) FROM lectures WHERE chapter_id = :chapterId")
    int countByChapterSync(String chapterId);

    @Query("SELECT COUNT(*) FROM lectures")
    int countSync();

    @Query("SELECT COUNT(*) FROM lectures WHERE completed = 1")
    int countCompletedSync();

    @Query("UPDATE lectures SET completed = :completed, completed_at = :completedAt, updated_at = :updatedAt WHERE id = :id")
    int updateCompletion(String id, boolean completed, long completedAt, long updatedAt);

    @Query("UPDATE lectures SET video_url = :url, updated_at = :updatedAt WHERE id = :id")
    int updateVideoUrl(String id, String url, long updatedAt);

    @Query("UPDATE lectures SET notes = :notes, updated_at = :updatedAt WHERE id = :id")
    int updateNotes(String id, String notes, long updatedAt);

    @Query("UPDATE lectures SET completed = 0, completed_at = 0, updated_at = :updatedAt WHERE chapter_id = :chapterId")
    int resetChapterProgress(String chapterId, long updatedAt);

    @Query("UPDATE lectures SET completed = 1, completed_at = :completedAt, updated_at = :updatedAt WHERE chapter_id = :chapterId AND completed = 0")
    int completeAllInChapter(String chapterId, long completedAt, long updatedAt);

    /** Removes lectures above the new total when the chapter shrinks. */
    @Query("DELETE FROM lectures WHERE chapter_id = :chapterId AND number > :keepUpTo")
    int deleteAbove(String chapterId, int keepUpTo);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Lecture lecture);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Lecture> lectures);

    @Update
    int update(Lecture lecture);

    @Update
    int updateAll(List<Lecture> lectures);

    @Query("DELETE FROM lectures WHERE id = :id")
    int deleteById(String id);

    @Query("DELETE FROM lectures")
    void deleteAll();
}
