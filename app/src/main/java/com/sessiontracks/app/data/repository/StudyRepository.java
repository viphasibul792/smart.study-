package com.sessiontracks.app.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.sessiontracks.app.R;
import com.sessiontracks.app.data.AppDatabase;
import com.sessiontracks.app.data.dao.ChapterDao;
import com.sessiontracks.app.data.dao.LectureDao;
import com.sessiontracks.app.data.dao.ProgressDao;
import com.sessiontracks.app.data.dao.SessionDao;
import com.sessiontracks.app.data.dao.SubjectDao;
import com.sessiontracks.app.data.entity.Chapter;
import com.sessiontracks.app.data.entity.Lecture;
import com.sessiontracks.app.data.entity.StudySession;
import com.sessiontracks.app.data.entity.Subject;
import com.sessiontracks.app.data.model.ChapterProgress;
import com.sessiontracks.app.data.model.ChapterWithProgress;
import com.sessiontracks.app.data.model.OverallStats;
import com.sessiontracks.app.data.model.SessionWithSubjects;
import com.sessiontracks.app.data.model.SubjectProgress;
import com.sessiontracks.app.data.model.SubjectWithProgress;
import com.sessiontracks.app.util.AppExecutors;
import com.sessiontracks.app.util.ColorUtils;
import com.sessiontracks.app.util.TimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Single source of truth for all study data. ViewModels talk only to this class;
 * every write happens on the disk executor and every read is exposed as LiveData
 * so the UI survives rotation and process recreation automatically.
 */
public class StudyRepository {

    private static final String TAG = "StudyRepository";

    private static volatile StudyRepository instance;

    private final Context appContext;
    private final AppDatabase database;
    private final SessionDao sessionDao;
    private final SubjectDao subjectDao;
    private final ChapterDao chapterDao;
    private final LectureDao lectureDao;
    private final ProgressDao progressDao;
    private final AppExecutors executors;

    private StudyRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.database = AppDatabase.getInstance(this.appContext);
        this.sessionDao = database.sessionDao();
        this.subjectDao = database.subjectDao();
        this.chapterDao = database.chapterDao();
        this.lectureDao = database.lectureDao();
        this.progressDao = database.progressDao();
        this.executors = AppExecutors.getInstance();
    }

    @NonNull
    public static StudyRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (StudyRepository.class) {
                if (instance == null) {
                    instance = new StudyRepository(context);
                }
            }
        }
        return instance;
    }

    /** Callback for operations whose outcome the UI needs to report. */
    public interface Callback<T> {
        void onResult(boolean success, @Nullable T data, @Nullable String errorMessage);
    }

    private <T> void deliver(@Nullable Callback<T> callback, boolean success,
                             @Nullable T data, @Nullable String error) {
        if (callback == null) {
            return;
        }
        executors.mainThread().execute(() -> callback.onResult(success, data, error));
    }

    @NonNull
    private String genericError() {
        return appContext.getString(R.string.error_db);
    }

    // ==================================================================
    // Dashboard
    // ==================================================================

    /**
     * Sessions with their subject cards and per-subject progress, combined from
     * three tables. Using a MediatorLiveData keeps the whole dashboard reactive:
     * ticking one lecture updates every percentage shown above it.
     */
    @NonNull
    public LiveData<List<SessionWithSubjects>> observeDashboard() {
        MediatorLiveData<List<SessionWithSubjects>> result = new MediatorLiveData<>();

        LiveData<List<StudySession>> sessions = sessionDao.observeAll();
        LiveData<List<Subject>> subjects = subjectDao.observeAll();
        LiveData<List<SubjectProgress>> progress = progressDao.observeSubjectProgress();

        final List<StudySession>[] latestSessions = new List[]{null};
        final List<Subject>[] latestSubjects = new List[]{null};
        final List<SubjectProgress>[] latestProgress = new List[]{null};

        Runnable combine = () -> {
            if (latestSessions[0] == null) {
                return;
            }
            result.setValue(buildDashboard(latestSessions[0],
                    latestSubjects[0] == null ? Collections.emptyList() : latestSubjects[0],
                    latestProgress[0] == null ? Collections.emptyList() : latestProgress[0]));
        };

        result.addSource(sessions, value -> {
            latestSessions[0] = value;
            combine.run();
        });
        result.addSource(subjects, value -> {
            latestSubjects[0] = value;
            combine.run();
        });
        result.addSource(progress, value -> {
            latestProgress[0] = value;
            combine.run();
        });
        return result;
    }

    @NonNull
    private List<SessionWithSubjects> buildDashboard(@NonNull List<StudySession> sessions,
                                                     @NonNull List<Subject> subjects,
                                                     @NonNull List<SubjectProgress> progressList) {
        Map<String, SubjectProgress> progressById = new HashMap<>();
        for (SubjectProgress progress : progressList) {
            progressById.put(progress.subjectId, progress);
        }
        Map<String, List<SubjectWithProgress>> bySession = new HashMap<>();
        for (Subject subject : subjects) {
            SubjectProgress progress = progressById.get(subject.getId());
            if (progress == null) {
                progress = new SubjectProgress(subject.getId(), 0, 0, 0);
            }
            List<SubjectWithProgress> list = bySession.get(subject.getSessionId());
            if (list == null) {
                list = new ArrayList<>();
                bySession.put(subject.getSessionId(), list);
            }
            list.add(new SubjectWithProgress(subject, progress));
        }
        List<SessionWithSubjects> out = new ArrayList<>(sessions.size());
        for (StudySession session : sessions) {
            List<SubjectWithProgress> list = bySession.get(session.getId());
            out.add(new SessionWithSubjects(session, list == null ? new ArrayList<>() : list));
        }
        return out;
    }

    @NonNull
    public LiveData<OverallStats> observeOverallStats() {
        return progressDao.observeOverallStats();
    }

    // ==================================================================
    // Sessions
    // ==================================================================

    @NonNull
    public LiveData<StudySession> observeSession(@NonNull String sessionId) {
        return sessionDao.observeById(sessionId);
    }

    public void createSession(@NonNull String name, int startMinute, int endMinute,
                              int color, boolean reminderEnabled, @Nullable Callback<String> callback) {
        executors.diskIO().execute(() -> {
            try {
                int position = sessionDao.nextPositionSync();
                StudySession session = new StudySession(UUID.randomUUID().toString(),
                        name.trim(), startMinute, endMinute, color, position);
                session.setReminderEnabled(reminderEnabled);
                sessionDao.insert(session);
                deliver(callback, true, session.getId(), null);
            } catch (Exception e) {
                Log.e(TAG, "createSession failed", e);
                deliver(callback, false, null, genericError());
            }
        });
    }

    public void updateSession(@NonNull StudySession session, @Nullable Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                session.setUpdatedAt(System.currentTimeMillis());
                sessionDao.update(session);
                deliver(callback, true, null, null);
            } catch (Exception e) {
                Log.e(TAG, "updateSession failed", e);
                deliver(callback, false, null, genericError());
            }
        });
    }

    public void deleteSession(@NonNull String sessionId, @Nullable Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                sessionDao.deleteById(sessionId);
                deliver(callback, true, null, null);
            } catch (Exception e) {
                Log.e(TAG, "deleteSession failed", e);
                deliver(callback, false, null, genericError());
            }
        });
    }

    /** Re-inserts a deleted session together with its whole subtree (undo support). */
    public void restoreSessionTree(@NonNull StudySession session,
                                   @NonNull List<Subject> subjects,
                                   @NonNull List<Chapter> chapters,
                                   @NonNull List<Lecture> lectures,
                                   @Nullable Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                database.runInTransaction(() -> {
                    sessionDao.insert(session);
                    subjectDao.insertAll(subjects);
                    chapterDao.insertAll(chapters);
                    lectureDao.insertAll(lectures);
                });
                deliver(callback, true, null, null);
            } catch (Exception e) {
                Log.e(TAG, "restoreSessionTree failed", e);
                deliver(callback, false, null, genericError());
            }
        });
    }

    /** Loads a whole session subtree so it can be restored after deletion. */
    public void loadSessionTree(@NonNull String sessionId, @NonNull Callback<SessionTree> callback) {
        executors.diskIO().execute(() -> {
            try {
                StudySession session = sessionDao.getByIdSync(sessionId);
                if (session == null) {
                    deliver(callback, false, null, genericError());
                    return;
                }
                List<Subject> subjects = subjectDao.getBySessionSync(sessionId);
                List<Chapter> chapters = new ArrayList<>();
                List<Lecture> lectures = new ArrayList<>();
                for (Subject subject : subjects) {
                    List<Chapter> subjectChapters = chapterDao.getBySubjectSync(subject.getId());
                    chapters.addAll(subjectChapters);
                    for (Chapter chapter : subjectChapters) {
                        lectures.addAll(lectureDao.getByChapterSync(chapter.getId()));
                    }
                }
                deliver(callback, true, new SessionTree(session, subjects, chapters, lectures), null);
            } catch (Exception e) {
                Log.e(TAG, "loadSessionTree failed", e);
                deliver(callback, false, null, genericError());
            }
        });
    }

    /** Immutable snapshot of a session and everything beneath it. */
    public static final class SessionTree {
        @NonNull
        public final StudySession session;
        @NonNull
        public final List<Subject> subjects;
        @NonNull
        public final List<Chapter> chapters;
        @NonNull
        public final List<Lecture> lectures;

        public SessionTree(@NonNull StudySession session, @NonNull List<Subject> subjects,
                           @NonNull List<Chapter> chapters, @NonNull List<Lecture> lectures) {
            this.session = session;
            this.subjects = subjects;
            this.chapters = chapters;
            this.lectures = lectures;
        }
    }

    // ==================================================================
    // Subjects
    // ==================================================================

    @NonNull
    public LiveData<Subject> observeSubject(@NonNull String subjectId) {
        return subjectDao.observeById(subjectId);
    }

    public void createSubject(@NonNull String sessionId, @NonNull String name,
                              int startMinute, int endMinute, int color,
                              @Nullable Callback<String> callback) {
        executors.diskIO().execute(() -> {
            try {
                int position = subjectDao.nextPositionSync(sessionId);
                Subject subject = new Subject(UUID.randomUUID().toString(), sessionId,
                        name.trim(), startMinute, endMinute, color, position);
                subjectDao.insert(subject);
                deliver(callback, true, subject.getId(), null);
            } catch (Exception e) {
                Log.e(TAG, "createSubject failed", e);
                deliver(callback, false, null, genericError());
            }
        });
    }

    public void updateSubject(@NonNull Subject subject, @Nullable Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                subject.setUpdatedAt(System.currentTimeMillis());
                subjectDao.update(subject);
                deliver(callback, true, null, null);
            } catch (Exception e) {
                Log.e(TAG, "updateSubject failed", e);
                deliver(callback, false, null, genericError());
            }
        });
    }

    public void deleteSubject(@NonNull String subjectId, @Nullable Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                subjectDao.deleteById(subjectId);
                deliver(callback, true, null, null);
            } catch (Exception e) {
                Log.e(TAG, "deleteSubject failed", e);
                deliver(callback, false, null, genericError());
            }
        });
    }

    // ==================================================================
    // Chapters
    // ==================================================================

    @NonNull
    public LiveData<Chapter> observeChapter(@NonNull String chapterId) {
        return chapterDao.observeById(chapterId);
    }

    /** Chapters of a subject joined with their progress, for the subject screen. */
    @NonNull
    public LiveData<List<ChapterWithProgress>> observeChaptersWithProgress(@NonNull String subjectId) {
        MediatorLiveData<List<ChapterWithProgress>> result = new MediatorLiveData<>();
        LiveData<List<Chapter>> chapters = chapterDao.observeBySubject(subjectId);
        LiveData<List<ChapterProgress>> progress = progressDao.observeChapterProgressForSubject(subjectId);

        final List<Chapter>[] latestChapters = new List[]{null};
        final List<ChapterProgress>[] latestProgress = new List[]{null};

        Runnable combine = () -> {
            if (latestChapters[0] == null) {
                return;
            }
            Map<String, ChapterProgress> byId = new HashMap<>();
            if (latestProgress[0] != null) {
                for (ChapterProgress item : latestProgress[0]) {
                    byId.put(item.chapterId, item);
                }
            }
            List<ChapterWithProgress> out = new ArrayList<>(latestChapters[0].size());
            for (Chapter chapter : latestChapters[0]) {
                ChapterProgress item = byId.get(chapter.getId());
                if (item == null) {
                    item = new ChapterProgress(chapter.getId(), 0, 0);
                }
                out.add(new ChapterWithProgress(chapter, item));
            }
            result.setValue(out);
        };

        result.addSource(chapters, value -> {
            latestChapters[0] = value;
            combine.run();
        });
        result.addSource(progress, value -> {
            latestProgress[0] = value;
            combine.run();
        });
        return result;
    }

    @NonNull
    public LiveData<ChapterProgress> observeChapterProgress(@NonNull String chapterId) {
        return progressDao.observeSingleChapterProgress(chapterId);
    }

    /**
     * Creates a chapter and materialises {@code totalLectures} lecture rows in the
     * same transaction, which is what makes the "type 20 → get 20 buttons" flow work.
     */
    public void createChapter(@NonNull String subjectId, @NonNull String number, @NonNull String name,
                              int totalLectures, @Nullable Callback<String> callback) {
        executors.diskIO().execute(() -> {
            try {
                final int safeTotal = clampLectureCount(totalLectures);
                int position = chapterDao.nextPositionSync(subjectId);
                Chapter chapter = new Chapter(UUID.randomUUID().toString(), subjectId,
                        number.trim(), name.trim(), safeTotal, position);
                database.runInTransaction(() -> {
                    chapterDao.insert(chapter);
                    lectureDao.insertAll(buildLectures(chapter.getId(), 1, safeTotal));
                });
                deliver(callback, true, chapter.getId(), null);
            } catch (Exception e) {
                Log.e(TAG, "createChapter failed", e);
                deliver(callback, false, null, genericError());
            }
        });
    }

    /**
     * Updates chapter metadata and grows/shrinks its lecture list to match the new
     * total. Growing preserves every existing link, note and tick; shrinking removes
     * only the trailing lectures.
     */
    public void updateChapter(@NonNull String chapterId, @NonNull String number, @NonNull String name,
                              int totalLectures, @Nullable Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                Chapter chapter = chapterDao.getByIdSync(chapterId);
                if (chapter == null) {
                    deliver(callback, false, null, genericError());
                    return;
                }
                final int safeTotal = clampLectureCount(totalLectures);
                chapter.setNumber(number.trim());
                chapter.setName(name.trim());
                chapter.setTotalLectures(safeTotal);
                chapter.setUpdatedAt(System.currentTimeMillis());
                database.runInTransaction(() -> {
                    chapterDao.update(chapter);
                    syncLectureRows(chapterId, safeTotal);
                });
                deliver(callback, true, null, null);
            } catch (Exception e) {
                Log.e(TAG, "updateChapter failed", e);
                deliver(callback, false, null, genericError());
            }
        });
    }

    /** Changes only the lecture count (used by the inline stepper on the chapter screen). */
    public void setLectureCount(@NonNull String chapterId, int totalLectures, @Nullable Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                final int safeTotal = clampLectureCount(totalLectures);
                database.runInTransaction(() -> {
                    chapterDao.updateTotalLectures(chapterId, safeTotal, System.currentTimeMillis());
                    syncLectureRows(chapterId, safeTotal);
                });
                deliver(callback, true, null, null);
            } catch (Exception e) {
                Log.e(TAG, "setLectureCount failed", e);
                deliver(callback, false, null, genericError());
            }
        });
    }

    public void updateChapterNotes(@NonNull String chapterId, @NonNull String notes,
                                   @Nullable Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                chapterDao.updateNotes(chapterId, notes, System.currentTimeMillis());
                deliver(callback, true, null, null);
            } catch (Exception e) {
                Log.e(TAG, "updateChapterNotes failed", e);
                deliver(callback, false, null, genericError());
            }
        });
    }

    public void deleteChapter(@NonNull String chapterId, @Nullable Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                chapterDao.deleteById(chapterId);
                deliver(callback, true, null, null);
            } catch (Exception e) {
                Log.e(TAG, "deleteChapter failed", e);
                deliver(callback, false, null, genericError());
            }
        });
    }

    @WorkerThread
    private void syncLectureRows(@NonNull String chapterId, int desiredTotal) {
        int existing = lectureDao.countByChapterSync(chapterId);
        if (desiredTotal > existing) {
            // Fill gaps: only insert numbers that do not exist yet.
            List<Lecture> current = lectureDao.getByChapterSync(chapterId);
            java.util.Set<Integer> present = new java.util.HashSet<>();
            for (Lecture lecture : current) {
                present.add(lecture.getNumber());
            }
            List<Lecture> toAdd = new ArrayList<>();
            for (int number = 1; number <= desiredTotal; number++) {
                if (!present.contains(number)) {
                    toAdd.add(new Lecture(UUID.randomUUID().toString(), chapterId, number));
                }
            }
            if (!toAdd.isEmpty()) {
                lectureDao.insertAll(toAdd);
            }
        } else if (desiredTotal < existing) {
            lectureDao.deleteAbove(chapterId, desiredTotal);
        }
    }

    @NonNull
    private List<Lecture> buildLectures(@NonNull String chapterId, int from, int to) {
        List<Lecture> lectures = new ArrayList<>(Math.max(0, to - from + 1));
        for (int number = from; number <= to; number++) {
            lectures.add(new Lecture(UUID.randomUUID().toString(), chapterId, number));
        }
        return lectures;
    }

    private int clampLectureCount(int requested) {
        int max = appContext.getResources().getInteger(R.integer.max_lectures_per_chapter);
        return Math.max(0, Math.min(requested, max));
    }

    // ==================================================================
    // Lectures
    // ==================================================================

    @NonNull
    public LiveData<List<Lecture>> observeLectures(@NonNull String chapterId) {
        return lectureDao.observeByChapter(chapterId);
    }

    public void setLectureCompleted(@NonNull String lectureId, boolean completed,
                                    @Nullable Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                long now = System.currentTimeMillis();
                lectureDao.updateCompletion(lectureId, completed, completed ? now : 0L, now);
                deliver(callback, true, null, null);
            } catch (Exception e) {
                Log.e(TAG, "setLectureCompleted failed", e);
                deliver(callback, false, null, genericError());
            }
        });
    }

    public void setLectureLink(@NonNull String lectureId, @NonNull String url,
                               @Nullable Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                lectureDao.updateVideoUrl(lectureId, url.trim(), System.currentTimeMillis());
                deliver(callback, true, null, null);
            } catch (Exception e) {
                Log.e(TAG, "setLectureLink failed", e);
                deliver(callback, false, null, genericError());
            }
        });
    }

    public void updateLecture(@NonNull Lecture lecture, @Nullable Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                lecture.setUpdatedAt(System.currentTimeMillis());
                lectureDao.update(lecture);
                deliver(callback, true, null, null);
            } catch (Exception e) {
                Log.e(TAG, "updateLecture failed", e);
                deliver(callback, false, null, genericError());
            }
        });
    }

    public void updateLectureNotes(@NonNull String lectureId, @NonNull String notes,
                                   @Nullable Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                lectureDao.updateNotes(lectureId, notes, System.currentTimeMillis());
                deliver(callback, true, null, null);
            } catch (Exception e) {
                Log.e(TAG, "updateLectureNotes failed", e);
                deliver(callback, false, null, genericError());
            }
        });
    }

    public void resetChapterProgress(@NonNull String chapterId, @Nullable Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                lectureDao.resetChapterProgress(chapterId, System.currentTimeMillis());
                deliver(callback, true, null, null);
            } catch (Exception e) {
                Log.e(TAG, "resetChapterProgress failed", e);
                deliver(callback, false, null, genericError());
            }
        });
    }

    public void completeAllInChapter(@NonNull String chapterId, @Nullable Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                long now = System.currentTimeMillis();
                lectureDao.completeAllInChapter(chapterId, now, now);
                deliver(callback, true, null, null);
            } catch (Exception e) {
                Log.e(TAG, "completeAllInChapter failed", e);
                deliver(callback, false, null, genericError());
            }
        });
    }

    /**
     * Applies a parsed bulk-import mapping.
     *
     * @param mapping       lecture number → URL
     * @param overwrite     when false, lectures that already have a link keep it
     * @param expandChapter when true the chapter grows so every mapped number exists
     * @return via callback: how many lectures actually received a link
     */
    public void applyBulkLinks(@NonNull String chapterId, @NonNull Map<Integer, String> mapping,
                               boolean overwrite, boolean expandChapter,
                               @Nullable Callback<Integer> callback) {
        executors.diskIO().execute(() -> {
            try {
                if (mapping.isEmpty()) {
                    deliver(callback, true, 0, null);
                    return;
                }
                int highest = 0;
                for (Integer number : mapping.keySet()) {
                    highest = Math.max(highest, number == null ? 0 : number);
                }
                final int requiredTotal = clampLectureCount(highest);
                final int[] applied = {0};

                database.runInTransaction(() -> {
                    Chapter chapter = chapterDao.getByIdSync(chapterId);
                    if (chapter == null) {
                        return;
                    }
                    if (expandChapter && requiredTotal > chapter.getTotalLectures()) {
                        chapterDao.updateTotalLectures(chapterId, requiredTotal, System.currentTimeMillis());
                        syncLectureRows(chapterId, requiredTotal);
                    }
                    List<Lecture> lectures = lectureDao.getByChapterSync(chapterId);
                    Map<Integer, Lecture> byNumber = new HashMap<>();
                    for (Lecture lecture : lectures) {
                        byNumber.put(lecture.getNumber(), lecture);
                    }
                    long now = System.currentTimeMillis();
                    List<Lecture> updates = new ArrayList<>();
                    for (Map.Entry<Integer, String> entry : mapping.entrySet()) {
                        Integer number = entry.getKey();
                        String url = entry.getValue();
                        if (number == null || url == null || url.trim().isEmpty()) {
                            continue;
                        }
                        Lecture lecture = byNumber.get(number);
                        if (lecture == null) {
                            continue;
                        }
                        if (!overwrite && lecture.hasLink()) {
                            continue;
                        }
                        lecture.setVideoUrl(url.trim());
                        lecture.setUpdatedAt(now);
                        updates.add(lecture);
                    }
                    if (!updates.isEmpty()) {
                        lectureDao.updateAll(updates);
                    }
                    applied[0] = updates.size();
                });
                deliver(callback, true, applied[0], null);
            } catch (Exception e) {
                Log.e(TAG, "applyBulkLinks failed", e);
                deliver(callback, false, null, genericError());
            }
        });
    }

    // ==================================================================
    // Seeding / maintenance
    // ==================================================================

    /**
     * Inserts the three default evening sessions the product ships with, but only
     * on a genuinely empty database so a user's own layout is never overwritten.
     */
    public void seedDefaultsIfEmpty(@Nullable Callback<Boolean> callback) {
        executors.diskIO().execute(() -> {
            try {
                if (sessionDao.countSync() > 0) {
                    deliver(callback, true, false, null);
                    return;
                }
                insertDefaultSessions();
                deliver(callback, true, true, null);
            } catch (Exception e) {
                Log.e(TAG, "seedDefaultsIfEmpty failed", e);
                deliver(callback, false, null, genericError());
            }
        });
    }

    /** Adds the default sessions again on top of whatever already exists. */
    public void restoreDefaultSessions(@Nullable Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                insertDefaultSessions();
                deliver(callback, true, null, null);
            } catch (Exception e) {
                Log.e(TAG, "restoreDefaultSessions failed", e);
                deliver(callback, false, null, genericError());
            }
        });
    }

    /**
     * The product ships with three evening sessions:
     * 7:00–8:00 PM, 9:00–10:00 PM and 11:00 PM–12:00 AM.
     */
    @WorkerThread
    private void insertDefaultSessions() {
        int base = sessionDao.nextPositionSync();
        int[][] times = {{19 * 60, 20 * 60}, {21 * 60, 22 * 60}, {23 * 60, 0}};
        int[] nameRes = {R.string.default_session_1, R.string.default_session_2, R.string.default_session_3};

        List<StudySession> defaults = new ArrayList<>(times.length);
        for (int i = 0; i < times.length; i++) {
            StudySession session = new StudySession(UUID.randomUUID().toString(),
                    appContext.getString(nameRes[i]), times[i][0], times[i][1],
                    ColorUtils.colorForIndex(appContext, base + i), base + i);
            defaults.add(session);
        }
        sessionDao.insertAll(defaults);
    }

    public void deleteAllData(@Nullable Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                database.runInTransaction(() -> {
                    lectureDao.deleteAll();
                    chapterDao.deleteAll();
                    subjectDao.deleteAll();
                    sessionDao.deleteAll();
                });
                deliver(callback, true, null, null);
            } catch (Exception e) {
                Log.e(TAG, "deleteAllData failed", e);
                deliver(callback, false, null, genericError());
            }
        });
    }

    /** Sessions whose reminder should be scheduled. Runs on the caller's worker thread. */
    @WorkerThread
    @NonNull
    public List<StudySession> getReminderSessionsSync() {
        try {
            return sessionDao.getReminderEnabledSync();
        } catch (Exception e) {
            Log.e(TAG, "getReminderSessionsSync failed", e);
            return Collections.emptyList();
        }
    }

    @WorkerThread
    @Nullable
    public StudySession getSessionSync(@NonNull String sessionId) {
        try {
            return sessionDao.getByIdSync(sessionId);
        } catch (Exception e) {
            return null;
        }
    }

    @WorkerThread
    @NonNull
    public List<Subject> getSubjectsSync(@NonNull String sessionId) {
        try {
            return subjectDao.getBySessionSync(sessionId);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /** Next upcoming session today, or null when none remain. */
    @WorkerThread
    @Nullable
    public StudySession getNextSessionSync() {
        try {
            List<StudySession> sessions = sessionDao.getAllSync();
            StudySession best = null;
            int bestDelta = Integer.MAX_VALUE;
            for (StudySession session : sessions) {
                int delta = TimeUtils.minutesUntil(session.getStartMinute());
                if (delta < bestDelta) {
                    bestDelta = delta;
                    best = session;
                }
            }
            return best;
        } catch (Exception e) {
            return null;
        }
    }

    @NonNull
    public AppExecutors executors() {
        return executors;
    }

    @NonNull
    public AppDatabase database() {
        return database;
    }
}
