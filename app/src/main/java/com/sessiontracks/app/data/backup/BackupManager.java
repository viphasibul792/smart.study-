package com.sessiontracks.app.data.backup;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.sessiontracks.app.BuildConfig;
import com.sessiontracks.app.R;
import com.sessiontracks.app.data.AppDatabase;
import com.sessiontracks.app.data.entity.Chapter;
import com.sessiontracks.app.data.entity.Lecture;
import com.sessiontracks.app.data.entity.StudySession;
import com.sessiontracks.app.data.entity.Subject;
import com.sessiontracks.app.data.repository.PreferenceManager;
import com.sessiontracks.app.util.AppExecutors;
import com.sessiontracks.app.util.TimeUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Exports and imports the whole study tree as a single .json document through the
 * Storage Access Framework, so no storage permission is ever required and the user
 * chooses exactly where the file lives (device, SD card, Drive, …).
 */
public class BackupManager {

    private static final String TAG = "BackupManager";
    /** Guard against a user picking a huge unrelated file. */
    private static final long MAX_IMPORT_BYTES = 32L * 1024 * 1024;

    public static final String MIME_JSON = "application/json";

    private final Context appContext;
    private final AppDatabase database;
    private final PreferenceManager preferences;
    private final AppExecutors executors;
    private final Gson gson;

    public BackupManager(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.database = AppDatabase.getInstance(this.appContext);
        this.preferences = PreferenceManager.getInstance(this.appContext);
        this.executors = AppExecutors.getInstance();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /** Result callback delivered on the main thread. */
    public interface BackupCallback<T> {
        void onResult(boolean success, @Nullable T data, @Nullable String errorMessage);
    }

    private <T> void deliver(@Nullable BackupCallback<T> callback, boolean success,
                             @Nullable T data, @Nullable String error) {
        if (callback == null) {
            return;
        }
        executors.mainThread().execute(() -> callback.onResult(success, data, error));
    }

    /** Suggested file name shown in the system "create document" dialog. */
    @NonNull
    public String suggestedFileName() {
        return appContext.getString(R.string.backup_file_name,
                TimeUtils.fileTimestamp(System.currentTimeMillis()));
    }

    // ==================================================================
    // Export
    // ==================================================================

    /** Serialises the database into the document the user picked. */
    public void exportTo(@NonNull Uri target, @Nullable BackupCallback<Integer> callback) {
        executors.diskIO().execute(() -> {
            OutputStream output = null;
            Writer writer = null;
            try {
                BackupModels.BackupFile file = buildBackup();
                ContentResolver resolver = appContext.getContentResolver();
                output = resolver.openOutputStream(target, "wt");
                if (output == null) {
                    deliver(callback, false, null, appContext.getString(R.string.backup_export_failed));
                    return;
                }
                writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
                gson.toJson(file, writer);
                writer.flush();
                preferences.setLastBackupTime(System.currentTimeMillis());
                deliver(callback, true, file.sessions.size(), null);
            } catch (IOException | SecurityException e) {
                Log.e(TAG, "export failed", e);
                deliver(callback, false, null, appContext.getString(R.string.backup_export_failed));
            } catch (Exception e) {
                Log.e(TAG, "export failed", e);
                deliver(callback, false, null, appContext.getString(R.string.backup_export_failed));
            } finally {
                closeQuietly(writer);
                closeQuietly(output);
            }
        });
    }

    /** Writes the backup to app-private cache and returns the file for sharing. */
    public void exportToCache(@NonNull BackupCallback<java.io.File> callback) {
        executors.diskIO().execute(() -> {
            Writer writer = null;
            try {
                java.io.File dir = new java.io.File(appContext.getCacheDir(), "exports");
                if (!dir.exists() && !dir.mkdirs()) {
                    deliver(callback, false, null, appContext.getString(R.string.backup_export_failed));
                    return;
                }
                java.io.File out = new java.io.File(dir, suggestedFileName());
                writer = new OutputStreamWriter(new java.io.FileOutputStream(out), StandardCharsets.UTF_8);
                gson.toJson(buildBackup(), writer);
                writer.flush();
                preferences.setLastBackupTime(System.currentTimeMillis());
                deliver(callback, true, out, null);
            } catch (Exception e) {
                Log.e(TAG, "cache export failed", e);
                deliver(callback, false, null, appContext.getString(R.string.backup_export_failed));
            } finally {
                closeQuietly(writer);
            }
        });
    }

    @WorkerThread
    @NonNull
    private BackupModels.BackupFile buildBackup() {
        BackupModels.BackupFile file = new BackupModels.BackupFile();
        file.exportedAt = System.currentTimeMillis();
        file.appVersion = BuildConfig.VERSION_NAME;

        List<StudySession> sessions = database.sessionDao().getAllSync();
        for (StudySession session : sessions) {
            BackupModels.BackupSession backupSession = new BackupModels.BackupSession();
            backupSession.id = session.getId();
            backupSession.name = session.getName();
            backupSession.startMinute = session.getStartMinute();
            backupSession.endMinute = session.getEndMinute();
            backupSession.color = session.getColor();
            backupSession.position = session.getPosition();
            backupSession.reminderEnabled = session.isReminderEnabled();
            backupSession.createdAt = session.getCreatedAt();
            backupSession.updatedAt = session.getUpdatedAt();

            for (Subject subject : database.subjectDao().getBySessionSync(session.getId())) {
                BackupModels.BackupSubject backupSubject = new BackupModels.BackupSubject();
                backupSubject.id = subject.getId();
                backupSubject.name = subject.getName();
                backupSubject.startMinute = subject.getStartMinute();
                backupSubject.endMinute = subject.getEndMinute();
                backupSubject.color = subject.getColor();
                backupSubject.position = subject.getPosition();
                backupSubject.createdAt = subject.getCreatedAt();
                backupSubject.updatedAt = subject.getUpdatedAt();

                for (Chapter chapter : database.chapterDao().getBySubjectSync(subject.getId())) {
                    BackupModels.BackupChapter backupChapter = new BackupModels.BackupChapter();
                    backupChapter.id = chapter.getId();
                    backupChapter.number = chapter.getNumber();
                    backupChapter.name = chapter.getName();
                    backupChapter.totalLectures = chapter.getTotalLectures();
                    backupChapter.notes = chapter.getNotes();
                    backupChapter.position = chapter.getPosition();
                    backupChapter.createdAt = chapter.getCreatedAt();
                    backupChapter.updatedAt = chapter.getUpdatedAt();

                    for (Lecture lecture : database.lectureDao().getByChapterSync(chapter.getId())) {
                        BackupModels.BackupLecture backupLecture = new BackupModels.BackupLecture();
                        backupLecture.id = lecture.getId();
                        backupLecture.number = lecture.getNumber();
                        backupLecture.title = lecture.getTitle();
                        backupLecture.videoUrl = lecture.getVideoUrl();
                        backupLecture.completed = lecture.isCompleted();
                        backupLecture.completedAt = lecture.getCompletedAt();
                        backupLecture.notes = lecture.getNotes();
                        backupLecture.updatedAt = lecture.getUpdatedAt();
                        backupChapter.lectures.add(backupLecture);
                    }
                    backupSubject.chapters.add(backupChapter);
                }
                backupSession.subjects.add(backupSubject);
            }
            file.sessions.add(backupSession);
        }
        return file;
    }

    // ==================================================================
    // Import
    // ==================================================================

    /**
     * Reads and validates a backup document without touching the database, so the
     * UI can show a confirmation dialog with real counts first.
     */
    public void inspect(@NonNull Uri source, @NonNull BackupCallback<BackupModels.BackupFile> callback) {
        executors.diskIO().execute(() -> {
            try {
                BackupModels.BackupFile file = read(source);
                if (file == null || !file.isValid()) {
                    deliver(callback, false, null, appContext.getString(R.string.backup_import_invalid));
                    return;
                }
                deliver(callback, true, file, null);
            } catch (JsonSyntaxException e) {
                Log.w(TAG, "invalid json", e);
                deliver(callback, false, null, appContext.getString(R.string.backup_import_invalid));
            } catch (Exception e) {
                Log.e(TAG, "inspect failed", e);
                deliver(callback, false, null, appContext.getString(R.string.backup_import_failed));
            }
        });
    }

    /**
     * Restores a previously inspected backup.
     *
     * @param replaceAll true wipes current data first; false merges by inserting the
     *                   backup contents with fresh ids.
     */
    public void restore(@NonNull BackupModels.BackupFile file, boolean replaceAll,
                        @Nullable BackupCallback<BackupModels.ImportSummary> callback) {
        executors.diskIO().execute(() -> {
            try {
                BackupModels.ImportSummary summary = applyBackup(file, replaceAll);
                deliver(callback, true, summary, null);
            } catch (Exception e) {
                Log.e(TAG, "restore failed", e);
                deliver(callback, false, null, appContext.getString(R.string.backup_import_failed));
            }
        });
    }

    /** Convenience: read + restore in one step. */
    public void importFrom(@NonNull Uri source, boolean replaceAll,
                           @Nullable BackupCallback<BackupModels.ImportSummary> callback) {
        executors.diskIO().execute(() -> {
            try {
                BackupModels.BackupFile file = read(source);
                if (file == null || !file.isValid()) {
                    deliver(callback, false, null, appContext.getString(R.string.backup_import_invalid));
                    return;
                }
                deliver(callback, true, applyBackup(file, replaceAll), null);
            } catch (JsonSyntaxException e) {
                deliver(callback, false, null, appContext.getString(R.string.backup_import_invalid));
            } catch (Exception e) {
                Log.e(TAG, "import failed", e);
                deliver(callback, false, null, appContext.getString(R.string.backup_import_failed));
            }
        });
    }

    @WorkerThread
    @Nullable
    private BackupModels.BackupFile read(@NonNull Uri source) throws IOException {
        ContentResolver resolver = appContext.getContentResolver();
        InputStream input = null;
        BufferedReader reader = null;
        try {
            input = resolver.openInputStream(source);
            if (input == null) {
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[8192];
            int read;
            long total = 0;
            while ((read = reader.read(buffer)) != -1) {
                total += read;
                if (total > MAX_IMPORT_BYTES) {
                    throw new IOException("Backup file too large");
                }
                builder.append(buffer, 0, read);
            }
            String json = builder.toString().trim();
            if (json.isEmpty()) {
                return null;
            }
            return gson.fromJson(json, BackupModels.BackupFile.class);
        } finally {
            closeQuietly(reader);
            closeQuietly(input);
        }
    }

    @WorkerThread
    @NonNull
    private BackupModels.ImportSummary applyBackup(@NonNull BackupModels.BackupFile file, boolean replaceAll) {
        final int[] counts = new int[4];

        database.runInTransaction(() -> {
            if (replaceAll) {
                database.lectureDao().deleteAll();
                database.chapterDao().deleteAll();
                database.subjectDao().deleteAll();
                database.sessionDao().deleteAll();
            }

            int sessionPosition = replaceAll ? 0 : database.sessionDao().nextPositionSync();
            List<StudySession> sessions = new ArrayList<>();
            List<Subject> subjects = new ArrayList<>();
            List<Chapter> chapters = new ArrayList<>();
            List<Lecture> lectures = new ArrayList<>();

            if (file.sessions == null) {
                return;
            }
            for (BackupModels.BackupSession backupSession : file.sessions) {
                if (backupSession == null) {
                    continue;
                }
                // Merging re-keys everything so a second import cannot collide.
                String sessionId = replaceAll && isUsableId(backupSession.id)
                        ? backupSession.id : UUID.randomUUID().toString();

                StudySession session = new StudySession();
                session.setId(sessionId);
                session.setName(BackupModels.safe(backupSession.name));
                session.setStartMinute(clampMinute(backupSession.startMinute));
                session.setEndMinute(clampMinute(backupSession.endMinute));
                session.setColor(backupSession.color);
                session.setPosition(replaceAll ? backupSession.position : sessionPosition++);
                session.setReminderEnabled(backupSession.reminderEnabled);
                session.setCreatedAt(backupSession.createdAt > 0 ? backupSession.createdAt : System.currentTimeMillis());
                session.setUpdatedAt(System.currentTimeMillis());
                sessions.add(session);
                counts[0]++;

                if (backupSession.subjects == null) {
                    continue;
                }
                for (BackupModels.BackupSubject backupSubject : backupSession.subjects) {
                    if (backupSubject == null) {
                        continue;
                    }
                    String subjectId = replaceAll && isUsableId(backupSubject.id)
                            ? backupSubject.id : UUID.randomUUID().toString();

                    Subject subject = new Subject();
                    subject.setId(subjectId);
                    subject.setSessionId(sessionId);
                    subject.setName(BackupModels.safe(backupSubject.name));
                    subject.setStartMinute(backupSubject.startMinute);
                    subject.setEndMinute(backupSubject.endMinute);
                    subject.setColor(backupSubject.color);
                    subject.setPosition(backupSubject.position);
                    subject.setCreatedAt(backupSubject.createdAt > 0 ? backupSubject.createdAt : System.currentTimeMillis());
                    subject.setUpdatedAt(System.currentTimeMillis());
                    subjects.add(subject);
                    counts[1]++;

                    if (backupSubject.chapters == null) {
                        continue;
                    }
                    for (BackupModels.BackupChapter backupChapter : backupSubject.chapters) {
                        if (backupChapter == null) {
                            continue;
                        }
                        String chapterId = replaceAll && isUsableId(backupChapter.id)
                                ? backupChapter.id : UUID.randomUUID().toString();

                        Chapter chapter = new Chapter();
                        chapter.setId(chapterId);
                        chapter.setSubjectId(subjectId);
                        chapter.setNumber(BackupModels.safe(backupChapter.number));
                        chapter.setName(BackupModels.safe(backupChapter.name));
                        chapter.setNotes(BackupModels.safe(backupChapter.notes));
                        chapter.setPosition(backupChapter.position);
                        chapter.setCreatedAt(backupChapter.createdAt > 0 ? backupChapter.createdAt : System.currentTimeMillis());
                        chapter.setUpdatedAt(System.currentTimeMillis());

                        int maxNumber = 0;
                        List<Lecture> chapterLectures = new ArrayList<>();
                        if (backupChapter.lectures != null) {
                            for (BackupModels.BackupLecture backupLecture : backupChapter.lectures) {
                                if (backupLecture == null || backupLecture.number < 1) {
                                    continue;
                                }
                                Lecture lecture = new Lecture();
                                lecture.setId(replaceAll && isUsableId(backupLecture.id)
                                        ? backupLecture.id : UUID.randomUUID().toString());
                                lecture.setChapterId(chapterId);
                                lecture.setNumber(backupLecture.number);
                                lecture.setTitle(BackupModels.safe(backupLecture.title));
                                lecture.setVideoUrl(BackupModels.safe(backupLecture.videoUrl));
                                lecture.setCompleted(backupLecture.completed);
                                lecture.setCompletedAt(backupLecture.completedAt);
                                lecture.setNotes(BackupModels.safe(backupLecture.notes));
                                lecture.setUpdatedAt(System.currentTimeMillis());
                                chapterLectures.add(lecture);
                                maxNumber = Math.max(maxNumber, backupLecture.number);
                            }
                        }
                        // Keep the declared total consistent with the rows actually present.
                        chapter.setTotalLectures(Math.max(backupChapter.totalLectures, maxNumber));
                        chapters.add(chapter);
                        lectures.addAll(chapterLectures);
                        counts[2]++;
                        counts[3] += chapterLectures.size();
                    }
                }
            }

            database.sessionDao().insertAll(sessions);
            database.subjectDao().insertAll(subjects);
            database.chapterDao().insertAll(chapters);
            database.lectureDao().insertAll(lectures);
        });

        return new BackupModels.ImportSummary(counts[0], counts[1], counts[2], counts[3]);
    }

    private static boolean isUsableId(@Nullable String id) {
        return id != null && !id.trim().isEmpty();
    }

    private static int clampMinute(int value) {
        if (value < 0) {
            return 0;
        }
        return value % TimeUtils.MINUTES_PER_DAY;
    }

    private static void closeQuietly(@Nullable java.io.Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
            // Nothing actionable.
        }
    }
}
