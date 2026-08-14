package com.sessiontracks.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.sessiontracks.app.data.entity.Subject;

import java.util.List;

@Dao
public interface SubjectDao {

    @Query("SELECT * FROM subjects ORDER BY position ASC")
    LiveData<List<Subject>> observeAll();

    @Query("SELECT * FROM subjects ORDER BY position ASC")
    List<Subject> getAllSync();

    @Query("SELECT * FROM subjects WHERE session_id = :sessionId ORDER BY position ASC")
    LiveData<List<Subject>> observeBySession(String sessionId);

    @Query("SELECT * FROM subjects WHERE session_id = :sessionId ORDER BY position ASC")
    List<Subject> getBySessionSync(String sessionId);

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    Subject getByIdSync(String id);

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    LiveData<Subject> observeById(String id);

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM subjects WHERE session_id = :sessionId")
    int nextPositionSync(String sessionId);

    @Query("SELECT COUNT(*) FROM subjects")
    int countSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Subject subject);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Subject> subjects);

    @Update
    int update(Subject subject);

    @Query("DELETE FROM subjects WHERE id = :id")
    int deleteById(String id);

    @Query("DELETE FROM subjects")
    void deleteAll();
}
