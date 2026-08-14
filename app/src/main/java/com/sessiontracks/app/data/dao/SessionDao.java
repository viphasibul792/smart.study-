package com.sessiontracks.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.sessiontracks.app.data.entity.StudySession;

import java.util.List;

@Dao
public interface SessionDao {

    @Query("SELECT * FROM sessions ORDER BY position ASC, start_minute ASC")
    LiveData<List<StudySession>> observeAll();

    @Query("SELECT * FROM sessions ORDER BY position ASC, start_minute ASC")
    List<StudySession> getAllSync();

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    StudySession getByIdSync(String id);

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    LiveData<StudySession> observeById(String id);

    @Query("SELECT COUNT(*) FROM sessions")
    int countSync();

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM sessions")
    int nextPositionSync();

    @Query("SELECT * FROM sessions WHERE reminder_enabled = 1")
    List<StudySession> getReminderEnabledSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(StudySession session);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<StudySession> sessions);

    @Update
    int update(StudySession session);

    @Delete
    int delete(StudySession session);

    @Query("DELETE FROM sessions WHERE id = :id")
    int deleteById(String id);

    @Query("DELETE FROM sessions")
    void deleteAll();
}
