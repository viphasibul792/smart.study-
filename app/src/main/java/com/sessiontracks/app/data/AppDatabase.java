package com.sessiontracks.app.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.sessiontracks.app.data.dao.ChapterDao;
import com.sessiontracks.app.data.dao.LectureDao;
import com.sessiontracks.app.data.dao.ProgressDao;
import com.sessiontracks.app.data.dao.SessionDao;
import com.sessiontracks.app.data.dao.SubjectDao;
import com.sessiontracks.app.data.entity.Chapter;
import com.sessiontracks.app.data.entity.Lecture;
import com.sessiontracks.app.data.entity.StudySession;
import com.sessiontracks.app.data.entity.Subject;

@androidx.room.Database(
        entities = {StudySession.class, Subject.class, Chapter.class, Lecture.class},
        version = 1,
        exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    private static final String TAG = "AppDatabase";
    public static final String DB_NAME = "session_tracks.db";

    private static volatile AppDatabase instance;

    public abstract SessionDao sessionDao();

    public abstract SubjectDao subjectDao();

    public abstract ChapterDao chapterDao();

    public abstract LectureDao lectureDao();

    public abstract ProgressDao progressDao();

    @NonNull
    public static AppDatabase getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = build(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    @NonNull
    private static AppDatabase build(@NonNull Context appContext) {
        return Room.databaseBuilder(appContext, AppDatabase.class, DB_NAME)
                // No released schema versions exist before v1, so a destructive
                // fallback can only ever trigger on a corrupted/downgraded file.
                .fallbackToDestructiveMigrationOnDowngrade()
                .addCallback(new Callback() {
                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        super.onCreate(db);
                        Log.i(TAG, "Database created");
                    }

                    @Override
                    public void onOpen(@NonNull SupportSQLiteDatabase db) {
                        super.onOpen(db);
                        // Cascading deletes rely on foreign keys being enforced.
                        db.execSQL("PRAGMA foreign_keys = ON");
                    }
                })
                .build();
    }

    /** Test/reset hook: drops the cached singleton so the next call reopens the file. */
    public static void clearInstance() {
        synchronized (AppDatabase.class) {
            if (instance != null && instance.isOpen()) {
                instance.close();
            }
            instance = null;
        }
    }
}
