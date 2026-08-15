package com.sessiontracks.app.data.model;

import androidx.annotation.NonNull;

import com.sessiontracks.app.data.entity.StudySession;
import com.sessiontracks.app.data.entity.Subject;

import java.util.ArrayList;
import java.util.List;

/**
 * UI-facing composite: a session together with its subject cards and each
 * subject's progress. Built in the repository so adapters never query the DB.
 */
public class SessionWithSubjects {

    @NonNull
    private final StudySession session;
    @NonNull
    private final List<SubjectWithProgress> subjects;

    public SessionWithSubjects(@NonNull StudySession session, @NonNull List<SubjectWithProgress> subjects) {
        this.session = session;
        this.subjects = subjects;
    }

    @NonNull
    public StudySession getSession() {
        return session;
    }

    @NonNull
    public List<SubjectWithProgress> getSubjects() {
        return subjects;
    }

    public int getSubjectCount() {
        return subjects.size();
    }

    public int getTotalLectures() {
        int total = 0;
        for (SubjectWithProgress s : subjects) {
            total += s.getTotalLectures();
        }
        return total;
    }

    public int getCompletedLectures() {
        int done = 0;
        for (SubjectWithProgress s : subjects) {
            done += s.getCompletedLectures();
        }
        return done;
    }

    public int getPercent() {
        int total = getTotalLectures();
        if (total <= 0) {
            return 0;
        }
        return Math.round((getCompletedLectures() * 100f) / total);
    }

    /** Subject names joined for reminder notifications and search. */
    @NonNull
    public String getSubjectNames() {
        List<String> names = new ArrayList<>(subjects.size());
        for (SubjectWithProgress s : subjects) {
            names.add(s.getSubject().getName());
        }
        return android.text.TextUtils.join(", ", names);
    }

    /** Convenience for wrapping plain subjects (used by search filtering). */
    @NonNull
    public static List<SubjectWithProgress> wrap(@NonNull List<Subject> subjects) {
        List<SubjectWithProgress> out = new ArrayList<>(subjects.size());
        for (Subject subject : subjects) {
            out.add(new SubjectWithProgress(subject, new SubjectProgress(subject.getId(), 0, 0, 0)));
        }
        return out;
    }
}
