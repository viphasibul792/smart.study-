package com.sessiontracks.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.sessiontracks.app.data.entity.Chapter;

import java.util.List;

@Dao
public interface ChapterDao {

    @Query("SELECT * FROM chapters ORDER BY position ASC")
    List<Chapter> getAllSync();

    @Query("SELECT * FROM chapters WHERE subject_id = :subjectId ORDER BY position ASC")
    LiveData<List<Chapter>> observeBySubject(String subjectId);

    @Query("SELECT * FROM chapters WHERE subject_id = :subjectId ORDER BY position ASC")
    List<Chapter> getBySubjectSync(String subjectId);

    @Query("SELECT * FROM chapters WHERE id = :id LIMIT 1")
    Chapter getByIdSync(String id);

    @Query("SELECT * FROM chapters WHERE id = :id LIMIT 1")
    LiveData<Chapter> observeById(String id);

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM chapters WHERE subject_id = :subjectId")
    int nextPositionSync(String subjectId);

    @Query("SELECT COUNT(*) FROM chapters")
    int countSync();

    @Query("UPDATE chapters SET notes = :notes, updated_at = :updatedAt WHERE id = :id")
    int updateNotes(String id, String notes, long updatedAt);

    @Query("UPDATE chapters SET total_lectures = :total, updated_at = :updatedAt WHERE id = :id")
    int updateTotalLectures(String id, int total, long updatedAt);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Chapter chapter);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Chapter> chapters);

    @Update
    int update(Chapter chapter);

    @Query("DELETE FROM chapters WHERE id = :id")
    int deleteById(String id);

    @Query("DELETE FROM chapters")
    void deleteAll();
}
